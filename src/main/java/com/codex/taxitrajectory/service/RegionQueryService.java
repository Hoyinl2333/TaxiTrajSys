package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.GPSPoint;
import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.result.RegionQueryResult;
import com.codex.taxitrajectory.repository.DataLoader;
import com.codex.taxitrajectory.utils.TimePartitionedSpatialIndex;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * f3区域查询服务
 */
@Service
public class RegionQueryService {

    private final DataLoader dataLoader;
    private final TimePartitionedSpatialIndex spatialIndex;

    // 索引构建状态管理
    private final Map<LocalDate, AtomicBoolean> indexBuildStatus = new ConcurrentHashMap<>();

    // 并行线程池，用于索引构建
    private final ForkJoinPool indexBuildPool = new ForkJoinPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    // 缓存区域查询结果
    private final Map<RegionQuery, Integer> countCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    public RegionQueryService(DataLoader dataLoader, TimePartitionedSpatialIndex spatialIndex) {
        this.dataLoader = dataLoader;
        this.spatialIndex = spatialIndex;

        // 启动异步任务预加载近期数据索引,暂不使用
        // asyncPreloadRecentIndices();
    }

    /**
     * 异步预加载近期日期的索引
     */
    private void asyncPreloadRecentIndices() {
        new Thread(() -> {
            try {
                // 预加载最近一天的索引
                LocalDate today = LocalDate.of(2008, 2, 8); // 数据集中的最后一天
                buildIndexForDay(today);

                // 预加载前一天的索引
                LocalDate yesterday = today.minusDays(1);
                buildIndexForDay(yesterday);

                System.out.println("预加载索引完成");
            } catch (Exception e) {
                System.err.println("预加载索引失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 查询区域出租车数量，直接调用dataLoader查询版本
     * @param query RegionQuery类请求
     * @return
     */
    private int countTaxisInRegionDirect(RegionQuery query) {
        // 调用getTaxisInRegionDirect 实现
            return getTaxisInRegionDirect(query).getTaxiCount();
    }

    /**
     * 查询指定时间和空间区域内的出租车轨迹信息（返回出租车数量和GPS点）
     * @param query 区域查询参数
     * @return RegionQueryResult 包含出租车数量与轨迹点集合
     */
    private RegionQueryResult getTaxisInRegionDirect(RegionQuery query) {
        Set<String> uniqueTaxiIds = new HashSet<>(); // 防止有一辆车在相近时刻出现在同一区域内被重复计数
        Set<GPSPoint> gpsPoints = new HashSet<>();

        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();
        System.out.println("共找到 " + allTaxiIds.size() + " 个出租车ID");

        for (String taxiId : allTaxiIds) {
            List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(
                    taxiId,
                    query.getStartTime(),
                    query.getEndTime()
            );

            for (TaxiRecord record : records) {
                double lon = record.getLongitude();
                double lat = record.getLatitude();

                if (lon >= query.getMinLongitude() && lon <= query.getMaxLongitude() &&
                        lat >= query.getMinLatitude() && lat <= query.getMaxLatitude()) {

                    // 第一次会被加入集合
                    uniqueTaxiIds.add(taxiId);

                    // 构造GPS点并加入集合
                    GPSPoint point = new GPSPoint(lon,lat,record.getTimestamp(),taxiId);
                    gpsPoints.add(point);

                    // 可以根据需求决定是否 break，比如只要一个点就停止，但这里我们只收集第一个点
                    break;
                }
            }
        }

        RegionQueryResult result = new RegionQueryResult();
        result.setTaxiCount(uniqueTaxiIds.size());
        result.setGpsPoints(gpsPoints);

        System.out.println("查询完成，区域内出租车数量：" + result.getTaxiCount() +
                "，匹配轨迹点数量：" + gpsPoints.size());

        return result;
    }


/** =========================== 以下是接口 ======================== **/

// TODO: 暂时弃用这个方法，请使用getTaxisInRegion

//    /**
//     * 使用Query对象查询区域内的出租车数量
//     */
//    public int countTaxisInRegion(RegionQuery query) {
//
//        return countTaxisInRegionDirect(query);

//    // TODO：这个优化实现有问题，暂不使用

//        // 检查缓存
//        if (countCache.containsKey(query)) {
//            return countCache.get(query);
//        }
//
//        double minLon = query.getMinLongitude();
//        double minLat = query.getMinLatitude();
//        double maxLon = query.getMaxLongitude();
//        double maxLat = query.getMaxLatitude();
//        LocalDateTime startTime = query.getStartTime();
//        LocalDateTime endTime = query.getEndTime();
//
//        // 使用时空索引查询满足条件的GPS点
//        List<GPSPoint> pointsInRegion = spatialIndex.query(
//                minLon, minLat, maxLon, maxLat, startTime, endTime
//        );
//
//        // 统计不同的出租车ID数量
//        Set<String> uniqueTaxiIds = pointsInRegion.stream()
//                .map(GPSPoint::getTaxiId)
//                .collect(Collectors.toSet());
//
//        int result = uniqueTaxiIds.size();
//
//        // 缓存结果
//        if (countCache.size() >= MAX_CACHE_SIZE) {
//            // 如果缓存已满，移除一个随机条目
//            countCache.remove(countCache.keySet().iterator().next());
//        }
//        countCache.put(query, result);
//
//        return result;
//    }


    /**
     * 获取区域内所有出租车信息
     * @param query
     * @return
     */
    public RegionQueryResult getTaxisInRegion(RegionQuery query) {

        return getTaxisInRegionDirect(query);

        //TODO: 优化版本有BUG 暂不使用
//        // 检查索引构建
//        System.out.println("开始查询，检查日期索引状态：");
//        LocalDate currentDate = query.getStartTime().toLocalDate();
//        LocalDate endDate = query.getEndTime().toLocalDate();
//        while (!currentDate.isAfter(endDate)) {
//            boolean indexExists = spatialIndex.isIndexBuilt(currentDate);
//            System.out.println("日期 " + currentDate + " 的索引已构建: " + indexExists);
//            currentDate = currentDate.plusDays(1);
//        }
//
//
//        double minLon = query.getMinLongitude();
//        double minLat = query.getMinLatitude();
//        double maxLon = query.getMaxLongitude();
//        double maxLat = query.getMaxLatitude();
//        LocalDateTime startTime = query.getStartTime();
//        LocalDateTime endTime = query.getEndTime();
//
//        // 使用时空索引查询满足条件的GPS点
//        List<GPSPoint> pointsInRegion = spatialIndex.query(
//                minLon, minLat, maxLon, maxLat, startTime, endTime
//        );
//
//        // 收集不同的出租车ID
//        return pointsInRegion.stream()
//                .map(GPSPoint::getTaxiId)
//                .collect(Collectors.toSet());
    }

    /**
     * 构建指定日期的索引（并行处理）
     */
    private void buildIndexForDay(LocalDate day) {
        // 检查该天的索引是否已经构建或正在构建
        AtomicBoolean building = indexBuildStatus.computeIfAbsent(day, d -> new AtomicBoolean(false));

        if (building.compareAndSet(false, true)) {
            try {
                System.out.println("开始构建 " + day + " 的索引");

                // 获取所有出租车ID
                Set<String> allTaxiIds = dataLoader.getAllTaxiIds();

                // 确定时间范围
                LocalDateTime dayStart = day.atStartOfDay();
                LocalDateTime dayEnd = day.plusDays(1).atStartOfDay().minusNanos(1);

                // 使用并行流处理索引构建
                indexBuildPool.submit(() ->
                        allTaxiIds.parallelStream().forEach(taxiId -> {
                            try {
                                // 获取该出租车在指定日期的记录
                                List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(
                                        taxiId, dayStart, dayEnd
                                );

                                // 将记录转换为GPSPoint并添加到索引
                                for (TaxiRecord record : records) {
                                    GPSPoint point = new GPSPoint(
                                            record.getLongitude(),
                                            record.getLatitude(),
                                            record.getTimestamp(),
                                            record.getTaxiId()
                                    );
                                    spatialIndex.insert(point);
                                }
                            } catch (Exception e) {
                                System.err.println("处理出租车 " + taxiId + " 数据时出错: " + e.getMessage());
                            }
                        })
                ).get(); // 等待完成

                // 标记索引构建完成
                spatialIndex.markIndexComplete(day);
                System.out.println(day + " 的索引构建完成");
            } catch (Exception e) {
                System.err.println("构建 " + day + " 索引失败: " + e.getMessage());
            } finally {
                building.set(false);
            }
        }
    }

    /**
     * 清除查询缓存
     */
    public void clearCache() {
        countCache.clear();
    }
}
