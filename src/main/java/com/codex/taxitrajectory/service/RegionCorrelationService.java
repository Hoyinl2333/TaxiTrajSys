package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionCorrelationQuery;
import com.codex.taxitrajectory.repository.DataLoader;
import com.codex.taxitrajectory.utils.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class RegionCorrelationService {

    private static final Logger logger = LoggerFactory.getLogger(RegionCorrelationService.class);

    // 从配置文件中读取日志开关，默认开启日志
    @Value("${logging.service.enabled:true}")
    private boolean loggingEnabled;

    private final DataLoader dataLoader;

    public RegionCorrelationService(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    /**
     * 分析不同时间段内两个指定区域间的车流量变化
     * @param query 查询参数
     * @return 每个时间槽内两个方向的车流量
     */
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeBetweenRegions(RegionCorrelationQuery query) {
        if (loggingEnabled) {
            logger.info("开始分析两个区域间车流量变化，查询参数：{}", query);
        }
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();
        double topLeftLongitude1 = query.getTopLeftLongitude1();
        double topLeftLatitude1 = query.getTopLeftLatitude1();
        double bottomRightLongitude1 = query.getBottomRightLongitude1();
        double bottomRightLatitude1 = query.getBottomRightLatitude1();
        double topLeftLongitude2 = query.getTopLeftLongitude2();
        double topLeftLatitude2 = query.getTopLeftLatitude2();
        double bottomRightLongitude2 = query.getBottomRightLongitude2();
        double bottomRightLatitude2 = query.getBottomRightLatitude2();

        Map<LocalDateTime, int[]> flowChangeMap = new TreeMap<>();
        long slotGenerationStart = System.currentTimeMillis();
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);
        if (loggingEnabled) {
            logger.info("生成 {} 个时间槽，耗时：{} ms",
                    timeSlots.size(), System.currentTimeMillis() - slotGenerationStart);
        }

        long analysisStart = System.currentTimeMillis();
        for (int i = 0; i < timeSlots.size() - 1; i++) {
            LocalDateTime slotStartTime = timeSlots.get(i);
            LocalDateTime slotEndTime = timeSlots.get(i + 1);

            int[] flow = analyzeTrafficFlowBetweenRegions(
                    slotStartTime, slotEndTime,
                    topLeftLongitude1, topLeftLatitude1,
                    bottomRightLongitude1, bottomRightLatitude1,
                    topLeftLongitude2, topLeftLatitude2,
                    bottomRightLongitude2, bottomRightLatitude2
            );
            if (loggingEnabled) {
                logger.debug("时间槽 {} 至 {} 的车流量：区域1→区域2={}, 区域2→区域1={}",
                        slotStartTime, slotEndTime, flow[0], flow[1]);
            }
            flowChangeMap.put(slotStartTime, flow);
        }
        long totalAnalysisTime = System.currentTimeMillis() - analysisStart;
        if (loggingEnabled) {
            logger.info("车流量分析完成，总耗时：{} ms", totalAnalysisTime);
        }
        return flowChangeMap;
    }

    /**
     * 统计两个指定区域间不同方向的车流量
     */
    public int[] analyzeTrafficFlowBetweenRegions(
            LocalDateTime start, LocalDateTime end,
            double topLeftLongitude1, double topLeftLatitude1,
            double bottomRightLongitude1, double bottomRightLatitude1,
            double topLeftLongitude2, double topLeftLatitude2,
            double bottomRightLongitude2, double bottomRightLatitude2) {

        if (loggingEnabled) {
            logger.debug("分析时间段 {} 至 {} 内的车流量", start, end);
        }
        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();
        int flowFromRegion1ToRegion2 = 0;
        int flowFromRegion2ToRegion1 = 0;

        // 遍历所有出租车，统计车流
        for (String taxiId : allTaxiIds) {
            List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(taxiId, start, end);
            boolean inRegion1 = false;
            boolean inRegion2 = false;
            boolean firstInRegion1 = false;

            // 遍历该出租车在时间段内的轨迹记录
            for (TaxiRecord record : records) {
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
                    // 如果还没进入区域2，则认为该出租车第一次处于区域1
                    if (!inRegion2) {
                        firstInRegion1 = true;
                    }
                }
                if (isInRegion2 && !inRegion2) {
                    inRegion2 = true;
                    // 如果还没进入区域1，则认为出租车第一次出现区域2
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
                    // 找到两个区域的记录后，不必继续分析该出租车
                    break;
                }
            }
        }
        return new int[]{flowFromRegion1ToRegion2, flowFromRegion2ToRegion1};
    }

    /**
     * 生成时间槽列表
     */
    private List<LocalDateTime> generateTimeSlots(
            LocalDateTime startTime, LocalDateTime endTime, int timeSlotMinutes) {
        List<LocalDateTime> timeSlots = new ArrayList<>();
        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            timeSlots.add(current);
            current = current.plusMinutes(timeSlotMinutes);
        }
        if (loggingEnabled) {
            logger.debug("生成时间槽列表：{}", timeSlots);
        }
        return timeSlots;
    }
}
