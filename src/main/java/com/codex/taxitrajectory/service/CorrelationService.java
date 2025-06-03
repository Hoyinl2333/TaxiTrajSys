package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.query.CorrelationQuery;
import com.codex.taxitrajectory.model.result.RegionCorrelationResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CorrelationService {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationService.class);

    @Value("${logging.service.enabled:true}")
    private boolean loggingEnabled;

    private final TaxiRepository taxiRepository;

    public CorrelationService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    /**
     * 分析不同时间段内两个指定区域间的车流量变化
     * @param query 查询参数
     * @return 封装了每个时间槽内两个方向车流量的结果对象
     */
    public RegionCorrelationResult analyzeTrafficFlowChangeBetweenRegions(CorrelationQuery.RegionCorrelationQuery query) {
        if (loggingEnabled) {
            logger.info("开始分析两个区域间车流量变化，查询参数：{}", query);
        }

        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();

        // 生成时间槽
        long slotGenerationStart = System.currentTimeMillis();
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);
        if (loggingEnabled) {
            logger.info("生成 {} 个时间槽，耗时：{} ms",
                    timeSlots.size(), System.currentTimeMillis() - slotGenerationStart);
        }

        // 存储每个时间槽的流量数据 [从区域1到区域2的流量, 从区域2到区域1的流量]
        ConcurrentHashMap<LocalDateTime, int[]> flowChangeMap = new ConcurrentHashMap<>();

        // 获取所有出租车ID
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (loggingEnabled) {
            logger.info("开始处理 {} 辆出租车的轨迹数据", allTaxiIds.size());
        }

        // 并行处理每辆出租车的轨迹数据
        allTaxiIds.parallelStream().forEach(taxiId -> {
            com.codex.taxitrajectory.model.core.TaxiRecord prevRecord = null;

            try (Stream<com.codex.taxitrajectory.model.core.TaxiRecord> trajectoryRecordStream = taxiRepository.streamRecordsByTaxiId(taxiId)) {

                // 按时间顺序处理每条记录
                for (com.codex.taxitrajectory.model.core.TaxiRecord record : (Iterable<com.codex.taxitrajectory.model.core.TaxiRecord>) trajectoryRecordStream::iterator) {

                    // 检查记录是否在查询时间范围内
                    for (LocalDateTime slotStartTime : timeSlots) {
                        LocalDateTime slotEndTime = slotStartTime.plusMinutes(timeSlotMinutes);

                        if (record.getTimestamp().isAfter(slotStartTime) && record.getTimestamp().isBefore(slotEndTime)) {

                            // 至少需要两条记录才能判断移动方向
                            if (prevRecord != null) {
                                int[] flow = analyzeTrafficFlowBetweenRegions(
                                        prevRecord,
                                        record,
                                        query.getRegion1().getMinLon(), query.getRegion1().getMaxLat(),
                                        query.getRegion1().getMaxLon(), query.getRegion1().getMinLat(),
                                        query.getRegion2().getMinLon(), query.getRegion2().getMaxLat(),
                                        query.getRegion2().getMaxLon(), query.getRegion2().getMinLat()
                                );

                                // 更新时间槽的流量数据
                                flowChangeMap.compute(slotStartTime, (key, existingFlow) -> {
                                    if (existingFlow == null) {
                                        return flow;
                                    }
                                    existingFlow[0] += flow[0];
                                    existingFlow[1] += flow[1];
                                    return existingFlow;
                                });
                            }

                            prevRecord = record;
                            break; // 一条记录只属于一个时间槽
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("处理出租车 {} 的轨迹时发生错误: {}", taxiId, e.getMessage(), e);
            }
        });

        // 构建结果对象
        RegionCorrelationResult result = new RegionCorrelationResult();
        result.setTrafficFlowChange(flowChangeMap);

        if (loggingEnabled) {
            logger.info("区域间车流量分析完成，结果包含 {} 个时间槽的数据", flowChangeMap.size());
        }

        return result;
    }

    /**
     * 分析指定矩形区域与其他区域的车流量随时间的变化
     * @param query 查询参数
     * @return 封装了每个时间槽内两个方向车流量的结果对象
     */
    public RegionCorrelationResult analyzeTrafficFlowChangeWithOtherRegions(CorrelationQuery.RegionSingleCorrelationQuery query) {
        if (loggingEnabled) {
            logger.info("开始分析指定区域与其他区域的车流量变化，查询参数：{}", query);
        }

        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();

        // 生成时间槽
        long slotGenerationStart = System.currentTimeMillis();
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);
        if (loggingEnabled) {
            logger.info("生成 {} 个时间槽，耗时：{} ms",
                    timeSlots.size(), System.currentTimeMillis() - slotGenerationStart);
        }

        // 存储每个时间槽的流量数据 [流入区域的流量, 流出区域的流量]
        ConcurrentHashMap<LocalDateTime, int[]> flowChangeMap = new ConcurrentHashMap<>();

        // 获取所有出租车ID
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (loggingEnabled) {
            logger.info("开始处理 {} 辆出租车的轨迹数据", allTaxiIds.size());
        }

        // 并行处理每辆出租车的轨迹数据
        allTaxiIds.parallelStream().forEach(taxiId -> {
            com.codex.taxitrajectory.model.core.TaxiRecord prevRecord = null;

            try (Stream<com.codex.taxitrajectory.model.core.TaxiRecord> trajectoryRecordStream = taxiRepository.streamRecordsByTaxiId(taxiId)) {

                // 按时间顺序处理每条记录
                for (com.codex.taxitrajectory.model.core.TaxiRecord record : (Iterable<com.codex.taxitrajectory.model.core.TaxiRecord>) trajectoryRecordStream::iterator) {

                    // 检查记录是否在查询时间范围内
                    for (LocalDateTime slotStartTime : timeSlots) {
                        LocalDateTime slotEndTime = slotStartTime.plusMinutes(timeSlotMinutes);

                        if (record.getTimestamp().isAfter(slotStartTime) && record.getTimestamp().isBefore(slotEndTime)) {

                            // 至少需要两条记录才能判断移动方向
                            if (prevRecord != null) {
                                int[] flow = analyzeTrafficFlowWithOtherRegions(
                                        prevRecord,
                                        record,
                                        query.getTopLeftLongitude(), query.getTopLeftLatitude(),
                                        query.getBottomRightLongitude(), query.getBottomRightLatitude()
                                );

                                // 更新时间槽的流量数据
                                flowChangeMap.compute(slotStartTime, (key, existingFlow) -> {
                                    if (existingFlow == null) {
                                        return flow;
                                    }
                                    existingFlow[0] += flow[0];
                                    existingFlow[1] += flow[1];
                                    return existingFlow;
                                });
                            }

                            prevRecord = record;
                            break; // 一条记录只属于一个时间槽
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("处理出租车 {} 的轨迹时发生错误: {}", taxiId, e.getMessage(), e);
            }
        });

        // 构建结果对象
        RegionCorrelationResult result = new RegionCorrelationResult();
        result.setTrafficFlowChange(flowChangeMap);

        if (loggingEnabled) {
            logger.info("区域间车流量分析完成，结果包含 {} 个时间槽的数据", flowChangeMap.size());
        }

        return result;
    }

    /**
     * 生成时间槽
     */
    private List<LocalDateTime> generateTimeSlots(LocalDateTime startTime, LocalDateTime endTime, int timeSlotMinutes) {
        List<LocalDateTime> timeSlots = new ArrayList<>();
        LocalDateTime currentTime = startTime;

        while (currentTime.isBefore(endTime)) {
            timeSlots.add(currentTime);
            currentTime = currentTime.plusMinutes(timeSlotMinutes);
        }

        return timeSlots;
    }

    /**
     * 统计两个指定区域间不同方向的车流量
     */
    private int[] analyzeTrafficFlowBetweenRegions(
            com.codex.taxitrajectory.model.core.TaxiRecord prevRecord,
            com.codex.taxitrajectory.model.core.TaxiRecord record,
            double minLon1, double maxLat1,
            double maxLon1, double minLat1,
            double minLon2, double maxLat2,
            double maxLon2, double minLat2) {

        int flowFromRegion1ToRegion2 = 0;
        int flowFromRegion2ToRegion1 = 0;

        // 判断上一位置和当前位置是否在区域内
        boolean wasInRegion1 = GeoUtils.isInRectangle(prevRecord.getLongitude(), prevRecord.getLatitude(),
                minLon1, maxLat1, maxLon1, minLat1);
        boolean isInRegion1 = GeoUtils.isInRectangle(record.getLongitude(), record.getLatitude(),
                minLon1, maxLat1, maxLon1, minLat1);
        boolean wasInRegion2 = GeoUtils.isInRectangle(prevRecord.getLongitude(), prevRecord.getLatitude(),
                minLon2, maxLat2, maxLon2, minLat2);
        boolean isInRegion2 = GeoUtils.isInRectangle(record.getLongitude(), record.getLatitude(),
                minLon2, maxLat2, maxLon2, minLat2);

        // 判断移动方向
        if (wasInRegion1 && !isInRegion1 && !wasInRegion2 && isInRegion2) {
            flowFromRegion1ToRegion2++;
        } else if (wasInRegion2 && !isInRegion2 && !wasInRegion1 && isInRegion1) {
            flowFromRegion2ToRegion1++;
        }

        return new int[]{flowFromRegion1ToRegion2, flowFromRegion2ToRegion1};
    }

    /**
     * 统计指定矩形区域与其他区域间不同方向的车流量
     */
    private int[] analyzeTrafficFlowWithOtherRegions(
            com.codex.taxitrajectory.model.core.TaxiRecord prevRecord,
            com.codex.taxitrajectory.model.core.TaxiRecord record,
            double topLeftLongitude, double topLeftLatitude,
            double bottomRightLongitude, double bottomRightLatitude) {

        int flowIntoRegion = 0;
        int flowOutOfRegion = 0;

        // 判断上一位置和当前位置是否在区域内
        boolean wasInRegion = GeoUtils.isInRectangle(prevRecord.getLongitude(), prevRecord.getLatitude(),
                topLeftLongitude, topLeftLatitude, bottomRightLongitude, bottomRightLatitude);
        boolean isInRegion = GeoUtils.isInRectangle(record.getLongitude(), record.getLatitude(),
                topLeftLongitude, topLeftLatitude, bottomRightLongitude, bottomRightLatitude);

        // 判断移动方向
        if (!wasInRegion && isInRegion) {
            flowIntoRegion++;
        } else if (wasInRegion && !isInRegion) {
            flowOutOfRegion++;
        }

        return new int[]{flowIntoRegion, flowOutOfRegion};
    }
}


