package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.TravelTimeQuery;
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * F9 通行时间分析服务。
 * 计算在给定单一时间段内，从区域A到区域B的最短通行路径及时间。
 */
@Service
public class TravelTimeService {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeService.class);

    @Value("${trajectory.analysis.min_trip_points:2}")
    private int minTripPoints;

    private final TaxiRepository taxiRepository;

    @Autowired
    public TravelTimeService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    /**
     * 分析最短通行时间的主方法（针对单一时间段）
     */
    public TravelTimeResult analyzeShortestTravelTime(TravelTimeQuery query) {
        log.info("开始分析最短通行时间, 区域A: {}, 区域B: {}, 查询时间段: {} 到 {}",
                query.getRegionA(), query.getRegionB(), query.getStartTime(), query.getEndTime());

        // 1. 获取所有出租车ID
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (CollectionUtils.isEmpty(allTaxiIds)) {
            log.warn("未能获取到任何出租车ID，分析中止。");
            return new TravelTimeResult();
        }
        log.info("获取到 {} 个出租车ID进行分析。", allTaxiIds.size());

        // 2. 初始化最终结果
        AtomicReference<TravelTimeResult> finalResult = new AtomicReference<>(new TravelTimeResult());

        // 3. 并行处理每辆出租车的轨迹
        allTaxiIds.parallelStream().forEach(taxiId -> {
            try {
                // 使用流式处理加载轨迹数据
                List<TaxiRecord> trajectory = new ArrayList<>();
                try (Stream<TaxiRecord> stream = taxiRepository.streamRecordsByTaxiId(taxiId)) {
                    stream.filter(record ->
                            record.getTimestamp() != null &&
                                    !record.getTimestamp().isBefore(query.getStartTime()) &&
                                    !record.getTimestamp().isAfter(query.getEndTime())
                    ).forEach(trajectory::add);
                }

                // 按时间排序
                trajectory.sort(Comparator.comparing(TaxiRecord::getTimestamp));

                // 4. 识别潜在行程
                List<PotentialTrip> trips = identifyPotentialTrips(trajectory, query.getRegionA(), query.getRegionB());

                // 5. 更新最短路径
                updateShortestPath(trips, trajectory, finalResult);

            } catch (Exception e) {
                log.error("处理出租车 {} 轨迹时出错: {}", taxiId, e.getMessage(), e);
            }
        });

        return finalResult.get();
    }

    /**
     * 识别潜在的行程 (保持原有算法不变)
     */
    private List<PotentialTrip> identifyPotentialTrips(List<TaxiRecord> trajectory, Region regionA, Region regionB) {
        List<PotentialTrip> potentialTrips = new ArrayList<>();
        int trajectorySize = trajectory.size();

        enum State {
            OUTSIDE_A, // 在区域A外
            INSIDE_A_LOOKING_FOR_B // 在区域A内，寻找进入B的点
        }

        State currentState = State.OUTSIDE_A;
        TaxiRecord lastPointInA = null;
        int lastPointInAIndex = -1;
        int potentialSegmentStartIndex = -1;

        for (int i = 0; i < trajectorySize; i++) {
            TaxiRecord currentPoint = trajectory.get(i);
            if (currentPoint == null || currentPoint.getTimestamp() == null) continue;

            boolean isInA = GeoUtils.isPointInRegion(currentPoint.getLatitude(), currentPoint.getLongitude(), regionA);
            boolean isInB = GeoUtils.isPointInRegion(currentPoint.getLatitude(), currentPoint.getLongitude(), regionB);

            switch (currentState) {
                case OUTSIDE_A:
                    if (isInA) {
                        currentState = State.INSIDE_A_LOOKING_FOR_B;
                        lastPointInA = currentPoint;
                        lastPointInAIndex = i;
                        potentialSegmentStartIndex = i;
                    }
                    break;

                case INSIDE_A_LOOKING_FOR_B:
                    if (isInB) {
                        LocalDateTime endTime = currentPoint.getTimestamp();
                        int endIndex = i;

                        if (lastPointInA == null || lastPointInAIndex < potentialSegmentStartIndex) {
                            currentState = State.OUTSIDE_A;
                            break;
                        }

                        LocalDateTime startTime = lastPointInA.getTimestamp();
                        int startIndex = lastPointInAIndex;

                        int pointCount = endIndex - startIndex + 1;
                        if (pointCount >= minTripPoints && !startTime.isAfter(endTime)) {
                            potentialTrips.add(new PotentialTrip(startIndex, endIndex, startTime, endTime));
                        }

                        currentState = State.OUTSIDE_A;
                        lastPointInA = null;
                        lastPointInAIndex = -1;
                        potentialSegmentStartIndex = -1;
                    } else if (isInA) {
                        lastPointInA = currentPoint;
                        lastPointInAIndex = i;
                    }
            }
        }
        return potentialTrips;
    }

    /**
     * 更新最短路径结果
     */
    private void updateShortestPath(List<PotentialTrip> trips, List<TaxiRecord> trajectory,
                                    AtomicReference<TravelTimeResult> finalResult) {
        for (PotentialTrip trip : trips) {
            Duration travelTime = Duration.between(trip.getStartTime(), trip.getEndTime());
            TravelTimeResult currentResult = finalResult.get();

            if (!currentResult.isFound() || travelTime.compareTo(currentResult.getMinTravelTime()) < 0) {
                List<TaxiRecord> path = trajectory.subList(trip.getStartIndex(), trip.getEndIndex() + 1);
                finalResult.set(new TravelTimeResult(path, travelTime));
            }
        }
    }

    @Data
    private static class PotentialTrip {
        private final int startIndex;
        private final int endIndex;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
    }
}