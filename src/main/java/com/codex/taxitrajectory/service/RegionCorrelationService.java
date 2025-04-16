package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionCorrelationQuery;
import com.codex.taxitrajectory.repository.DataLoader;
import com.codex.taxitrajectory.utils.GeoUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RegionCorrelationService {

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
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);

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

            flowChangeMap.put(slotStartTime, flow);
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
        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();
        int flowFromRegion1ToRegion2 = 0;
        int flowFromRegion2ToRegion1 = 0;

        for (String taxiId : allTaxiIds) {
            List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(taxiId, start, end);
            boolean inRegion1 = false;
            boolean inRegion2 = false;
            boolean firstInRegion1 = false;

            for (TaxiRecord record : records) {
                double longitude = record.getLongitude();
                double latitude = record.getLatitude();

                boolean isInRegion1 = GeoUtils.isInRectangle(longitude, latitude, topLeftLongitude1, topLeftLatitude1, bottomRightLongitude1, bottomRightLatitude1);
                boolean isInRegion2 = GeoUtils.isInRectangle(longitude, latitude, topLeftLongitude2, topLeftLatitude2, bottomRightLongitude2, bottomRightLatitude2);

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

        return timeSlots;
    }
}