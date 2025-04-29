package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.core.TimeInterval;
import com.codex.taxitrajectory.model.query.TravelTimeQuery; // 假设 Query 类在此包下
import com.codex.taxitrajectory.model.result.ShortestPathInfo;
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // 用于读取配置
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils; // 用于检查集合是否为空

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * F9 通行时间分析服务。
 * 计算在给定时间段内，从区域A到区域B的最短通行路径及时间。
 * (版本：完善验证、错误处理、日志和优化)
 */
@Service
public class TravelTimeService {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeService.class);

    // 从配置文件读取最小行程点数，默认值为 2
    @Value("${trajectory.analysis.min_trip_points:2}")
    private int minTripPoints;

    private final TaxiRepository taxiRepository;

    @Autowired
    public TravelTimeService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    /**
     * 分析最短通行时间的主方法。
     *
     * @param query 查询参数
     * @return 分析结果
     */
    public TravelTimeResult analyzeShortestTravelTime(TravelTimeQuery query) {
        // 1. 参数验证
        if (!isValidQuery(query)) {
            return new TravelTimeResult(Collections.emptyMap()); // 返回空结果
        }
        log.info("开始分析最短通行时间, 区域A: {}, 区域B: {}, 查询时间段数量: {}",
                query.getRegionA(), query.getRegionB(), query.getTimeIntervals().size());

        // 2. 获取出租车 ID
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (CollectionUtils.isEmpty(allTaxiIds)) {
            log.warn("未能获取到任何出租车 ID，分析中止。");
            return new TravelTimeResult(Collections.emptyMap());
        }
        log.info("获取到 {} 个出租车 ID 进行分析。", allTaxiIds.size());

        // 3. 计算总体时间范围
        LocalDateTime overallStartTime, overallEndTime;
        try {
            overallStartTime = query.getTimeIntervals().stream().map(TimeInterval::getStartTime).min(LocalDateTime::compareTo)
                    .orElseThrow(() -> new IllegalArgumentException("无法确定最早开始时间"));
            overallEndTime = query.getTimeIntervals().stream().map(TimeInterval::getEndTime).max(LocalDateTime::compareTo)
                    .orElseThrow(() -> new IllegalArgumentException("无法确定最晚结束时间"));
            log.debug("计算出的总体轨迹查询时间范围: {} 到 {}", overallStartTime, overallEndTime);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("计算总体时间范围失败: {}", e.getMessage(), e);
            return new TravelTimeResult(Collections.emptyMap());
        }

        // 4. 初始化结果 Map
        List<TimeInterval> analysisIntervals = query.getTimeIntervals();
        Map<TimeInterval, ShortestPathInfo> shortestPathsByInterval = new ConcurrentHashMap<>(analysisIntervals.size());
        analysisIntervals.forEach(interval -> shortestPathsByInterval.put(interval, new ShortestPathInfo())); // 初始化为未找到

        // 5. 并行处理
        log.info("开始并行处理 {} 辆出租车的轨迹...", allTaxiIds.size());
        long startTimeMillis = System.currentTimeMillis(); // 记录开始时间

        allTaxiIds.parallelStream().forEach(taxiId -> {
            try {
                processSingleTaxi(taxiId, query.getRegionA(), query.getRegionB(),
                        overallStartTime, overallEndTime, analysisIntervals,
                        shortestPathsByInterval);
            } catch (Exception e) {
                // 捕获单车处理中的未知异常，记录并继续处理其他车辆
                log.error("处理出租车 {} 时发生未预期异常: {}", taxiId, e.getMessage(), e);
            }
        });

        long endTimeMillis = System.currentTimeMillis();
        log.info("所有出租车处理完成，总耗时: {} ms", (endTimeMillis - startTimeMillis));

        // 6. 返回结果
        return new TravelTimeResult(shortestPathsByInterval);
    }

    /**
     * 处理单个出租车的轨迹分析逻辑。
     */
    private void processSingleTaxi(String taxiId, Region regionA, Region regionB,
                                   LocalDateTime overallStartTime, LocalDateTime overallEndTime,
                                   List<TimeInterval> analysisIntervals,
                                   Map<TimeInterval, ShortestPathInfo> shortestPathsByInterval) {

        // 5.1 获取轨迹数据
        List<TaxiRecord> trajectory = taxiRepository.getRecordsByTimeRange(taxiId, overallStartTime, overallEndTime);

        // 假设 Repository 返回的列表已按时间排序。如果不是，需要在这里排序：
        // if (trajectory != null) { trajectory.sort(Comparator.comparing(TaxiRecord::getTimestamp)); }

        // 5.2 基本检查
        if (CollectionUtils.isEmpty(trajectory) || trajectory.size() < minTripPoints) {
            return; // 轨迹为空或点数不足，直接返回
        }

        // 5.3 识别潜在行程
        List<PotentialTrip> potentialTrips = identifyPotentialTrips(taxiId, trajectory, regionA, regionB);

        // 5.4 过滤行程并更新最短路径
        if (!potentialTrips.isEmpty()) {
            filterAndupdateShortestPaths(potentialTrips, analysisIntervals, shortestPathsByInterval, trajectory);
        }
    }


    /**
     * 识别潜在的行程 (满足点数约束，起点终点在轨迹内)。
     */
    private List<PotentialTrip> identifyPotentialTrips(String taxiId, List<TaxiRecord> trajectory, Region regionA, Region regionB) {
        List<PotentialTrip> potentialTrips = new ArrayList<>();
        boolean inRegionA = false;
        TaxiRecord entryPointA = null;
        int entryPointAIndex = -1;
        int trajectorySize = trajectory.size();

        for (int i = 0; i < trajectorySize; i++) {
            TaxiRecord currentPoint = trajectory.get(i);
            if (currentPoint == null || currentPoint.getTimestamp() == null) continue;

            boolean isInA, isInB;
            try {
                // 直接使用静态方法，无需担心线程安全
                isInA = GeoUtils.isPointInRegion(currentPoint.getLatitude(), currentPoint.getLongitude(), regionA);
                isInB = GeoUtils.isPointInRegion(currentPoint.getLatitude(), currentPoint.getLongitude(), regionB);
            } catch (Exception e) {
                log.error("[{}] GeoUtils.isPointInRegion 发生错误，跳过此点: {}", taxiId, e.getMessage()); // 保留 GeoUtils 错误日志
                continue;
            }

            if (!inRegionA && isInA) {
                inRegionA = true; entryPointA = currentPoint; entryPointAIndex = i;
            } else if (inRegionA) {
                if (isInB) {
                    int pointCount = i - entryPointAIndex + 1;
                    if (pointCount >= minTripPoints) {
                        LocalDateTime startTime = entryPointA.getTimestamp();
                        LocalDateTime endTime = currentPoint.getTimestamp();
                        if (!startTime.isAfter(endTime)) {
                            // 存储索引和时间，不存路径列表
                            potentialTrips.add(new PotentialTrip(entryPointAIndex, i, startTime, endTime));
                        }
                        inRegionA = false; // 重置状态
                    }
                    // else: 点数不足，保持 inRegionA
                } else if (!isInA) { // 离开 A 且不在 B
                    inRegionA = false; // 重置状态
                }
            }
        }
        return potentialTrips;
    }

    /**
     * 过滤潜在行程，只保留起点终点在同一分析时间段内的，并更新最短路径 Map。
     * @param trajectory 原始轨迹列表，用于在需要时提取路径
     */
    private void filterAndupdateShortestPaths(List<PotentialTrip> potentialTrips,
                                              List<TimeInterval> analysisIntervals,
                                              Map<TimeInterval, ShortestPathInfo> shortestPathsByInterval,
                                              List<TaxiRecord> trajectory) { // 传入原始轨迹

        for (PotentialTrip potentialTrip : potentialTrips) {
            LocalDateTime startTime = potentialTrip.getStartTime();
            LocalDateTime endTime = potentialTrip.getEndTime();

            // 查找完全包含此行程的时间段
            TimeInterval assignedInterval = findContainingInterval(startTime, endTime, analysisIntervals);

            if (assignedInterval != null) {
                Duration duration = Duration.between(startTime, endTime);

                // 原子更新 Map
                shortestPathsByInterval.compute(assignedInterval, (intervalKey, existingInfo) -> {
                    if (existingInfo == null || !existingInfo.isFound() || duration.compareTo(existingInfo.getMinTravelTime()) < 0) {
                        // *** 需要更新时，才提取子列表 ***
                        List<TaxiRecord> tripPath;
                        try {
                            // 添加索引边界检查
                            if (potentialTrip.getStartIndex() >= 0 && potentialTrip.getEndIndex() < trajectory.size() && potentialTrip.getStartIndex() <= potentialTrip.getEndIndex()) {
                                tripPath = new ArrayList<>(trajectory.subList(potentialTrip.getStartIndex(), potentialTrip.getEndIndex() + 1));
                            } else {
                                log.error("提取轨迹子列表时索引无效: start={}, end={}, trajectorySize={}",
                                        potentialTrip.getStartIndex(), potentialTrip.getEndIndex(), trajectory.size());
                                return existingInfo; // 索引无效，不更新，返回旧值
                            }
                        } catch (Exception e) {
                            log.error("提取轨迹子列表时发生异常: {}", e.getMessage(), e);
                            return existingInfo; // 提取失败，不更新，返回旧值
                        }
                        // log.trace("更新时间段 [{}] 的最短路径，新耗时: {}", intervalKey, duration); // 移除 trace 日志
                        return new ShortestPathInfo(tripPath, duration); // 使用包含路径的构造函数
                    } else {
                        return existingInfo; // 保持旧值
                    }
                });
            }
        }
    }

    /**
     * 查找第一个完全包含给定开始和结束时间的分析时间段。
     */
    private TimeInterval findContainingInterval(LocalDateTime startTime, LocalDateTime endTime, List<TimeInterval> analysisIntervals) {
        for (TimeInterval interval : analysisIntervals) {
            // 使用闭区间 [start, end] 判断
            if (!startTime.isBefore(interval.getStartTime()) && !endTime.isAfter(interval.getEndTime())) {
                return interval;
            }
        }
        return null;
    }

    /**
     * 验证查询参数是否有效。
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
        if (CollectionUtils.isEmpty(query.getTimeIntervals())) {
            log.error("查询参数中的时间段列表 (TimeIntervals) 为空。");
            return false;
        }
        // 检查每个时间段的有效性
        for (TimeInterval interval : query.getTimeIntervals()) {
            if (interval == null || interval.getStartTime() == null || interval.getEndTime() == null) {
                log.error("查询参数中的时间段列表包含 null 或无效的时间段 (startTime 或 endTime 为 null)。");
                return false;
            }
            if (interval.getStartTime().isAfter(interval.getEndTime())) {
                log.error("查询参数中的时间段无效：开始时间 {} 晚于结束时间 {}。", interval.getStartTime(), interval.getEndTime());
                return false;
            }
        }
        return true;
    }

    /**
     * 验证区域对象坐标是否有效（基础检查）。
     */
    private boolean isValidRegion(Region region) {
        // 添加对经纬度范围的基础检查，可以根据实际地理范围调整
        // 例如：北京经度约 115.7 - 117.4, 纬度约 39.4 - 41.6
        return region != null &&
                region.getMinLon() < region.getMaxLon() && // 最小经度 < 最大经度
                region.getMinLat() < region.getMaxLat() && // 最小纬度 < 最大纬度
                region.getMinLon() >= -180 && region.getMaxLon() <= 180 && // 经度范围
                region.getMinLat() >= -90 && region.getMaxLat() <= 90;   // 纬度范围
    }


    // --- 内部类定义 ---

    @Data
    private static class PotentialTrip {
        private int startIndex;
        private int endIndex;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public PotentialTrip(int startIndex, int endIndex, LocalDateTime startTime, LocalDateTime endTime) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

}