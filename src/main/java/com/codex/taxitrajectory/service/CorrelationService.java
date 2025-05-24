package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.CorrelationQuery.RegionCorrelationQuery;
import com.codex.taxitrajectory.model.query.CorrelationQuery.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.model.result.CorrelationResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CorrelationService {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationService.class);

    @Value("${logging.service.enabled:true}")
    private boolean loggingEnabled;

    private final TaxiRepository taxiRepository;

    // 缓存已加载的出租车轨迹数据（出租车 ID -> 时间戳排序的轨迹数据）
    private final Map<String, NavigableMap<LocalDateTime, TaxiRecord>> taxiDataCache = new ConcurrentHashMap<>();

    public CorrelationService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    /**
     * 分析不同时间段内两个指定区域间的车流量变化
     * @param query 查询参数
     * @return 封装了每个时间槽内两个方向车流量的结果对象
     */
    public CorrelationResult analyzeTrafficFlowChangeBetweenRegions(RegionCorrelationQuery query) {
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

        // 预加载所有需要的数据到缓存
        long preloadStart = System.currentTimeMillis();
        preloadTaxiData(startTime, endTime);
        if (loggingEnabled) {
            logger.info("数据预加载完成，耗时：{} ms", System.currentTimeMillis() - preloadStart);
        }

        // 并行处理每个时间槽
        long analysisStart = System.currentTimeMillis();
        Map<LocalDateTime, int[]> flowChangeMap = timeSlots.parallelStream()
                .collect(Collectors.toMap(
                        slotStartTime -> slotStartTime,
                        slotStartTime -> {
                            Region region1 = query.getRegion1();
                            Region region2 = query.getRegion2();

                            // 调用私有辅助方法时，传递Region对象的边界值
                            // 根据约定“左上角经度小，纬度大”
                            // และ GeoUtils.isInRectangle 的参数顺序 (lon, lat, topLeftLon, topLeftLat, bottomRightLon, bottomRightLat)
                            // 我们需要将 Region 的 min/max 映射到 topLeft/bottomRight 概念
                            return analyzeTrafficFlowBetweenRegions(
                                    slotStartTime,
                                    slotStartTime.plusMinutes(timeSlotMinutes),
                                    region1.getMinLon(), // topLeftLongitude1
                                    region1.getMaxLat(), // topLeftLatitude1 (纬度大的是top)
                                    region1.getMaxLon(), // bottomRightLongitude1
                                    region1.getMinLat(), // bottomRightLatitude1 (纬度小的是bottom)
                                    region2.getMinLon(), // topLeftLongitude2
                                    region2.getMaxLat(), // topLeftLatitude2
                                    region2.getMaxLon(), // bottomRightLongitude2
                                    region2.getMinLat()  // bottomRightLatitude2
                            );
                        }
                ));

        long totalAnalysisTime = System.currentTimeMillis() - analysisStart;
        if (loggingEnabled) {
            logger.info("车流量分析完成，总耗时：{} ms", totalAnalysisTime);
        }

        CorrelationResult result = new CorrelationResult();
        result.setTrafficFlowChange(flowChangeMap);
        return result;
    }

    /**
     * 分析指定矩形区域与其他区域的车流量随时间的变化
     * @param query 查询参数
     * @return 封装了每个时间槽内两个方向车流量的结果对象
     */
    public CorrelationResult analyzeTrafficFlowChangeWithOtherRegions(RegionSingleCorrelationQuery query) {
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

        // 预加载所有需要的数据到缓存
        long preloadStart = System.currentTimeMillis();
        preloadTaxiData(startTime, endTime);
        if (loggingEnabled) {
            logger.info("数据预加载完成，耗时：{} ms", System.currentTimeMillis() - preloadStart);
        }

        // 并行处理每个时间槽
        long analysisStart = System.currentTimeMillis();
        Map<LocalDateTime, int[]> flowChangeMap = timeSlots.parallelStream()
                .collect(Collectors.toMap(
                        slotStartTime -> slotStartTime,
                        slotStartTime -> analyzeTrafficFlowWithOtherRegions(
                                slotStartTime,
                                slotStartTime.plusMinutes(timeSlotMinutes),
                                query.getTopLeftLongitude(),
                                query.getTopLeftLatitude(),
                                query.getBottomRightLongitude(),
                                query.getBottomRightLatitude()
                        )
                ));

        long totalAnalysisTime = System.currentTimeMillis() - analysisStart;
        if (loggingEnabled) {
            logger.info("车流量分析完成，总耗时：{} ms", totalAnalysisTime);
        }

        CorrelationResult result = new CorrelationResult();
        result.setTrafficFlowChange(flowChangeMap);
        return result;
    }

    /**
     * 预加载指定时间范围内的所有出租车数据到缓存
     */
    private void preloadTaxiData(LocalDateTime startTime, LocalDateTime endTime) {
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        allTaxiIds.parallelStream().forEach(taxiId -> {
            if (!taxiDataCache.containsKey(taxiId)) {
                List<TaxiRecord> records = taxiRepository.getRecordsByTimeRange(taxiId, startTime, endTime);
                NavigableMap<LocalDateTime, TaxiRecord> sortedMap = new TreeMap<>();
                records.forEach(record -> sortedMap.put(record.getTimestamp(), record));
                taxiDataCache.put(taxiId, sortedMap);
            }
        });
    }

    /**
     * 统计两个指定区域间不同方向的车流量
     */
    private int[] analyzeTrafficFlowBetweenRegions(
            LocalDateTime start, LocalDateTime end,
            double topLeftLongitude1, double topLeftLatitude1,
            double bottomRightLongitude1, double bottomRightLatitude1,
            double topLeftLongitude2, double topLeftLatitude2,
            double bottomRightLongitude2, double bottomRightLatitude2) {

        if (loggingEnabled) {
            logger.debug("分析时间段 {} 至 {} 内的车流量", start, end);
        }

        int flowFromRegion1ToRegion2 = 0;
        int flowFromRegion2ToRegion1 = 0;

        // 遍历所有出租车，统计车流
        for (Map.Entry<String, NavigableMap<LocalDateTime, TaxiRecord>> entry : taxiDataCache.entrySet()) {
            NavigableMap<LocalDateTime, TaxiRecord> records = entry.getValue();
            NavigableMap<LocalDateTime, TaxiRecord> timeRangeRecords = records.subMap(start, true, end, true);

            boolean inRegion1 = false;
            boolean inRegion2 = false;
            boolean firstInRegion1 = false;

            // 遍历该出租车在时间段内的轨迹记录
            for (TaxiRecord record : timeRangeRecords.values()) {
                double longitude = record.getLongitude();
                double latitude = record.getLatitude();

                boolean isInRegion1 = GeoUtils.isInRectangle(longitude, latitude,
                        topLeftLongitude1, topLeftLatitude1,
                        bottomRightLongitude1, bottomRightLatitude1);
                boolean isInRegion2 = GeoUtils.isInRectangle(longitude, latitude,
                        topLeftLongitude2, topLeftLatitude2,
                        bottomRightLongitude2, bottomRightLatitude2);

                if (isInRegion1 && !inRegion1) {
                    inRegion1 = true;
                    if (!inRegion2) {
                        firstInRegion1 = true;
                    }
                }
                if (isInRegion2 && !inRegion2) {
                    inRegion2 = true;
                    if (!inRegion1) {
                        firstInRegion1 = false;
                    }
                }
                if (inRegion1 && inRegion2) {
                    if (firstInRegion1) {
                        flowFromRegion1ToRegion2++;
                    } else {
                        flowFromRegion2ToRegion1++;
                    }
                    break;
                }
            }
        }
        return new int[]{flowFromRegion1ToRegion2, flowFromRegion2ToRegion1};
    }

    /**
     * 统计指定矩形区域与其他区域间不同方向的车流量
     */
    private int[] analyzeTrafficFlowWithOtherRegions(
            LocalDateTime start, LocalDateTime end,
            double topLeftLongitude, double topLeftLatitude,
            double bottomRightLongitude, double bottomRightLatitude) {

        if (loggingEnabled) {
            logger.debug("分析时间段 {} 至 {} 内的车流量", start, end);
        }

        int flowIntoRegion = 0;
        int flowOutOfRegion = 0;

        // 遍历所有出租车，统计车流
        for (Map.Entry<String, NavigableMap<LocalDateTime, TaxiRecord>> entry : taxiDataCache.entrySet()) {
            NavigableMap<LocalDateTime, TaxiRecord> records = entry.getValue();
            NavigableMap<LocalDateTime, TaxiRecord> timeRangeRecords = records.subMap(start, true, end, true);

            boolean inRegion = false;
            boolean firstInRegion = false;

            // 遍历该出租车在时间段内的轨迹记录
            for (TaxiRecord record : timeRangeRecords.values()) {
                double longitude = record.getLongitude();
                double latitude = record.getLatitude();

                boolean isInRegion = GeoUtils.isInRectangle(longitude, latitude,
                        topLeftLongitude, topLeftLatitude,
                        bottomRightLongitude, bottomRightLatitude);

                if (isInRegion && !inRegion) {
                    inRegion = true;
                    if (!firstInRegion) {
                        flowIntoRegion++;
                        firstInRegion = true;
                    }
                }
                if (!isInRegion && inRegion) {
                    flowOutOfRegion++;
                    inRegion = false;
                }
            }
        }
        return new int[]{flowIntoRegion, flowOutOfRegion};
    }

    /**
     * 生成时间槽列表
     */
    private List<LocalDateTime> generateTimeSlots(
            LocalDateTime startTime, LocalDateTime endTime, int timeSlotMinutes) {
        List<LocalDateTime> timeSlots = new ArrayList<>();
        LocalDateTime current = startTime;
        while (current.isBefore(endTime)) {
            timeSlots.add(current);
            current = current.plusMinutes(timeSlotMinutes);
        }
        if (loggingEnabled) {
            logger.debug("生成时间槽列表：{}", timeSlots);
        }
        return timeSlots;
    }
}
