package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
// import com.codex.taxitrajectory.model.core.TimeInterval; // TimeInterval 类本身可能还在 PotentialTrip 等地方使用
import com.codex.taxitrajectory.model.query.TravelTimeQuery;
// 引入你重命名后的 TravelTimeResult 类
import com.codex.taxitrajectory.model.result.TravelTimeResult; // 注意：这个TravelTimeResult现在是原来的ShortestPathInfo

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * F9 通行时间分析服务。
 * 计算在给定单一时间段内，从区域A到区域B的最短通行路径及时间。
 * (版本：使用重命名后的结果类，完善验证、错误处理、日志和优化)
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
     * 分析最短通行时间的主方法（针对单一时间段），返回重命名后的 TravelTimeResult。
     *
     * @param query 查询参数 (包含单一时间段)
     * @return 分析结果 (TravelTimeResult 对象，原 ShortestPathInfo)
     */
    public TravelTimeResult analyzeShortestTravelTime(TravelTimeQuery query) {

        // 1. 开始，参数校验已经通过Query
        log.info("开始分析最短通行时间, 区域A: {}, 区域B: {}, 查询时间段: {} 到 {}",
                query.getRegionA(), query.getRegionB(), query.getStartTime(), query.getEndTime());

        // 2. 获取出租车 ID
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (CollectionUtils.isEmpty(allTaxiIds)) {
            log.warn("未能获取到任何出租车 ID，分析中止。");
            return new TravelTimeResult(); // 返回未找到结果
        }
        log.info("获取到 {} 个出租车 ID 进行分析。", allTaxiIds.size());

        // 3. 总体时间范围就是查询的时间范围
        LocalDateTime overallStartTime = query.getStartTime();
        LocalDateTime overallEndTime = query.getEndTime();
        log.debug("使用的轨迹查询时间范围: {} 到 {}", overallStartTime, overallEndTime);

        // 4. 初始化最终结果持有者
        // 使用 AtomicReference 来线程安全地更新唯一的 TravelTimeResult 结果
        AtomicReference<TravelTimeResult> finalResultHolder = new AtomicReference<>(new TravelTimeResult());

        // 5. 并行处理
        log.info("开始并行处理 {} 辆出租车的轨迹...", allTaxiIds.size());
        long startTimeMillis = System.currentTimeMillis();

        allTaxiIds.parallelStream().forEach(taxiId -> {
            try {
                processSingleTaxi(taxiId, query.getRegionA(), query.getRegionB(),
                        overallStartTime, overallEndTime, finalResultHolder);
            } catch (Exception e) {
                log.error("处理出租车 {} 时发生未预期异常: {}", taxiId, e.getMessage(), e);
            }
        });

        long endTimeMillis = System.currentTimeMillis();
        log.info("所有出租车处理完成，总耗时: {} ms", (endTimeMillis - startTimeMillis));

        // 6. 返回最终结果
        return finalResultHolder.get(); // 直接返回 TravelTimeResult 对象
    }

    /**
     * 处理单个出租车的轨迹分析逻辑，并尝试更新共享的最短路径结果。
     */
    private void processSingleTaxi(String taxiId, Region regionA, Region regionB,
                                   LocalDateTime queryStartTime, LocalDateTime queryEndTime,
                                   AtomicReference<TravelTimeResult> finalResultHolder) { // 参数类型改为 TravelTimeResult

        // 5.1 获取轨迹数据 (按整个查询时间范围获取)
        List<TaxiRecord> trajectory = taxiRepository.getRecordsByTimeRange(taxiId, queryStartTime, queryEndTime);

        // 假设 Repository 返回的列表已按时间排序。如果不是，需要在这里排序：
        // if (trajectory != null) { trajectory.sort(Comparator.comparing(TaxiRecord::getTimestamp)); }

        // 5.2 基本检查
        if (CollectionUtils.isEmpty(trajectory) || trajectory.size() < minTripPoints) {
            log.trace("[{}] 轨迹为空或点数不足 ({})，跳过。", taxiId, trajectory == null ? 0 : trajectory.size());
            return;
        }
        log.trace("[{}] 获取到 {} 个轨迹点。", taxiId, trajectory.size());

        // 5.3 识别潜在行程 (使用更新后的逻辑，startTime是离开A的时间)
        List<PotentialTrip> potentialTrips = identifyPotentialTrips(taxiId, trajectory, regionA, regionB);
        log.trace("[{}] 识别到 {} 个潜在行程。", taxiId, potentialTrips.size());

        // 5.4 过滤行程 (基于单一查询时间段) 并更新最终的最短路径
        if (!potentialTrips.isEmpty()) {
            filterAndUpdateSingleShortestPath(potentialTrips, queryStartTime, queryEndTime, trajectory, finalResultHolder); // 参数类型改为 TravelTimeResult
        }
    }


    /**
     * 识别潜在的行程 (满足点数约束，起点终点在轨迹内)。
     * 修改：startTime 定义为离开区域 A 的时间点（最后一个在 A 区域内的点的时间）
     * (此方法与你上次确认修改后的代码一致)
     */
    private List<PotentialTrip> identifyPotentialTrips(String taxiId, List<TaxiRecord> trajectory, Region regionA, Region regionB) {
        List<PotentialTrip> potentialTrips = new ArrayList<>();
        int trajectorySize = trajectory.size();

        enum State {
            OUTSIDE_A, // 在区域 A 外
            INSIDE_A_LOOKING_FOR_B // 在区域 A 内，寻找进入 B 的点
        }

        State currentState = State.OUTSIDE_A;
        TaxiRecord lastPointInA = null;
        int lastPointInAIndex = -1;
        int potentialSegmentStartIndex = -1;

        for (int i = 0; i < trajectorySize; i++) {
            TaxiRecord currentPoint = trajectory.get(i);
            if (currentPoint == null || currentPoint.getTimestamp() == null) continue;

            boolean isInA, isInB;
            try {
                isInA = GeoUtils.isPointInRegion(currentPoint.getLatitude(), currentPoint.getLongitude(), regionA);
                isInB = GeoUtils.isPointInRegion(currentPoint.getLatitude(), currentPoint.getLongitude(), regionB);
            } catch (Exception e) {
                log.error("[{}] GeoUtils.isPointInRegion 发生错误，跳过此点: {}", taxiId, e.getMessage());
                continue;
            }

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
                            log.warn("[{}] 找到进入B的点，但在之前的A段落中未能找到有效的最后一个A点。SegmentStartIndex: {}, LastPointAIndex: {}",
                                    taxiId, potentialSegmentStartIndex, lastPointInAIndex);
                            currentState = State.OUTSIDE_A;
                            lastPointInA = null;
                            lastPointInAIndex = -1;
                            potentialSegmentStartIndex = -1;
                            break;
                        }

                        LocalDateTime startTime = lastPointInA.getTimestamp();
                        int startIndex = lastPointInAIndex;

                        int pointCount = endIndex - startIndex + 1;
                        if (pointCount >= minTripPoints) {
                            if (!startTime.isAfter(endTime)) {
                                potentialTrips.add(new PotentialTrip(startIndex, endIndex, startTime, endTime));
                                log.trace("[{}] 找到潜在行程 (A出->B进): 始点索引={}, 终点索引={}, 时长={}",
                                        taxiId, startIndex, endIndex, Duration.between(startTime, endTime));
                            } else {
                                log.warn("[{}] 找到潜在行程，但开始时间 ({}) 晚于结束时间 ({})，跳过。", taxiId, startTime, endTime);
                            }
                        } else {
                            log.trace("[{}] 找到潜在行程 (A出->B进)，但点数不足 ({})，跳过。", taxiId, pointCount);
                        }

                        currentState = State.OUTSIDE_A;
                        lastPointInA = null;
                        lastPointInAIndex = -1;
                        potentialSegmentStartIndex = -1;

                    } else if (isInA) {
                        lastPointInA = currentPoint;
                        lastPointInAIndex = i;

                    } else { // !isInA && !isInB
                        currentState = State.OUTSIDE_A;
                        lastPointInA = null;
                        lastPointInAIndex = -1;
                        potentialSegmentStartIndex = -1;
                    }
                    break;
            }
        }

        return potentialTrips;
    }


    /**
     * 过滤潜在行程 (基于单一查询时间段)，并原子地更新最终的最短路径结果。
     */
    private void filterAndUpdateSingleShortestPath(List<PotentialTrip> potentialTrips,
                                                   LocalDateTime queryStartTime, LocalDateTime queryEndTime,
                                                   List<TaxiRecord> trajectory,
                                                   AtomicReference<TravelTimeResult> finalResultHolder) { // 参数类型改为 TravelTimeResult

        for (PotentialTrip potentialTrip : potentialTrips) {
            LocalDateTime startTime = potentialTrip.getStartTime();
            LocalDateTime endTime = potentialTrip.getEndTime();

            // 过滤：行程的起始时间必须在查询的单一时间段内 [queryStartTime, queryEndTime]
            if (!startTime.isBefore(queryStartTime) && !startTime.isAfter(queryEndTime)) {
                Duration duration = Duration.between(startTime, endTime);

                // 原子更新最终的最短路径信息
                while (true) {
                    TravelTimeResult existingResult = finalResultHolder.get(); // 类型改为 TravelTimeResult

                    // 检查当前行程是否比已找到的最短行程更短 (或者这是找到的第一个行程)
                    // 这里需要调用 TravelTimeResult (原 ShortestPathInfo) 的 isFound() 方法和 getMinTravelTime() 方法
                    if (!existingResult.isFound() || duration.compareTo(existingResult.getMinTravelTime()) < 0) {
                        // 发现更短的路径，准备创建新的 TravelTimeResult (原 ShortestPathInfo)
                        List<TaxiRecord> tripPath;
                        try {
                            // 提取轨迹子列表，并进行边界检查
                            if (potentialTrip.getStartIndex() >= 0 && potentialTrip.getEndIndex() < trajectory.size() && potentialTrip.getStartIndex() <= potentialTrip.getEndIndex()) {
                                tripPath = new ArrayList<>(trajectory.subList(potentialTrip.getStartIndex(), potentialTrip.getEndIndex() + 1));
                            } else {
                                log.error("提取轨迹子列表时索引无效: start={}, end={}, trajectorySize={}",
                                        potentialTrip.getStartIndex(), potentialTrip.getEndIndex(), trajectory.size());
                                break; // 索引无效，跳出 while 循环，不尝试更新
                            }
                        } catch (Exception e) {
                            log.error("提取轨迹子列表时发生异常: {}", e.getMessage(), e);
                            break; // 提取失败，跳出 while 循环，不尝试更新
                        }

                        // 创建新的 TravelTimeResult 对象 (使用原 ShortestPathInfo 的构造函数)
                        TravelTimeResult newResult = new TravelTimeResult(tripPath, duration);

                        // 尝试原子更新：如果 holder 中的值仍然是 existingResult，则将其替换为 newResult
                        if (finalResultHolder.compareAndSet(existingResult, newResult)) {
                            log.trace("发现并更新最短路径，新耗时: {}", duration);
                            break; // 更新成功，跳出 while 循环
                        }
                        // 如果 compareAndSet 失败，说明在获取 existingResult 后，holder 的值被其他线程修改了
                        // while 循环会再次执行，获取最新的值并重新判断和尝试更新
                    } else {
                        // 当前行程时间不比已找到的最短时间短，无需更新
                        break; // 跳出 while 循环
                    }
                } // end while
            }
        }
    }


    /**
     * 验证查询参数是否有效（适配单一时间段）。
     * (此方法保持不变)
     */
    private boolean isValidQuery(TravelTimeQuery query) {
        if (query == null) {
            log.error("查询参数对象 (TravelTimeQuery) 为 null。");
            return false;
        }
        if (query.getRegionA() == null || !isValidRegion(query.getRegionA())) {
            log.error("查询参数中的区域 A (RegionA) 为 null 或无效。");
            return false;
        }
        if (query.getRegionB() == null || !isValidRegion(query.getRegionB())) {
            log.error("查询参数中的区域 B (RegionB) 为 null 或无效。");
            return false;
        }
        if (query.getStartTime() == null || query.getEndTime() == null) {
            log.error("查询参数中的时间段 (startTime 或 endTime) 为 null。");
            return false;
        }
        if (query.getStartTime().isAfter(query.getEndTime())) {
            log.error("查询参数中的时间段无效：开始时间 {} 晚于结束时间 {}。", query.getStartTime(), query.getEndTime());
            return false;
        }
        return true;
    }

    /**
     * 验证区域对象坐标是否有效（基础检查）。
     * (此方法保持不变)
     */
    private boolean isValidRegion(Region region) {
        return region != null &&
                region.getMinLon() < region.getMaxLon() &&
                region.getMinLat() < region.getMaxLat() &&
                region.getMinLon() >= -180 && region.getMaxLon() <= 180 &&
                region.getMinLat() >= -90 && region.getMaxLat() <= 90;
    }


    // --- 内部类定义 ---

    @Data
    private static class PotentialTrip {
        private int startIndex;
        private int endIndex;
        private LocalDateTime startTime; // 这是离开A区域的时间点（最后一个在A的点的时间）
        private LocalDateTime endTime;   // 这是进入B区域的时间点（第一个在B的点的时间）

        public PotentialTrip(int startIndex, int endIndex, LocalDateTime startTime, LocalDateTime endTime) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}