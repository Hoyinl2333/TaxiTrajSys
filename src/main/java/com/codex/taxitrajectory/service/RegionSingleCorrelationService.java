package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.repository.DataLoader;
import com.codex.taxitrajectory.utils.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RegionSingleCorrelationService {
    private static final Logger logger = LoggerFactory.getLogger(RegionSingleCorrelationService.class);

    @Value("${logging.service.enabled:true}")
    private boolean loggingEnabled;

    private final DataLoader dataLoader;

    public RegionSingleCorrelationService(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    /**
     * 分析指定矩形区域与其他区域的车流量随时间的变化
     * @param query 查询参数
     * @return 每个时间槽内两个方向的车流量
     */
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeWithOtherRegions(RegionSingleCorrelationQuery query) {
        if (loggingEnabled) {
            logger.info("开始分析指定区域与其他区域的车流量变化，查询参数：{}", query);
        }
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();
        double topLeftLongitude = query.getTopLeftLongitude();
        double topLeftLatitude = query.getTopLeftLatitude();
        double bottomRightLongitude = query.getBottomRightLongitude();
        double bottomRightLatitude = query.getBottomRightLatitude();

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

            int[] flow = analyzeTrafficFlowWithOtherRegions(
                    slotStartTime, slotEndTime,
                    topLeftLongitude, topLeftLatitude,
                    bottomRightLongitude, bottomRightLatitude
            );
            if (loggingEnabled) {
                logger.debug("时间槽 {} 至 {} 的车流量：进入区域={}, 离开区域={}",
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
     * 统计指定矩形区域与其他区域间不同方向的车流量
     */
    public int[] analyzeTrafficFlowWithOtherRegions(
            LocalDateTime start, LocalDateTime end,
            double topLeftLongitude, double topLeftLatitude,
            double bottomRightLongitude, double bottomRightLatitude) {

        if (loggingEnabled) {
            logger.debug("分析时间段 {} 至 {} 内的车流量", start, end);
        }
        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();
        int flowIntoRegion = 0;
        int flowOutOfRegion = 0;

        // 遍历所有出租车，统计车流
        for (String taxiId : allTaxiIds) {
            List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(taxiId, start, end);
            boolean inRegion = false;
            boolean firstInRegion = false;

            // 遍历该出租车在时间段内的轨迹记录
            for (TaxiRecord record : records) {
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