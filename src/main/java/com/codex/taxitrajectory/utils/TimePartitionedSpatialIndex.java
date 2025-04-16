package com.codex.taxitrajectory.utils;

import com.codex.taxitrajectory.model.GPSPoint;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class TimePartitionedSpatialIndex {
    // 日期分区索引
    private final Map<LocalDate, RTreeIndex> dailyIndices = new ConcurrentHashMap<>();
    private final Set<LocalDate> completedDates = ConcurrentHashMap.newKeySet();

    // 细粒度的网格索引，用于快速查找热点区域
    private final Map<String, Set<GPSPoint>> gridCache = new ConcurrentHashMap<>();

    // 读写锁，保证线程安全
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();

    private static final double GRID_SIZE = 0.01; // 约1公里大小的网格

    /**
     * 插入GPS点到索引
     */
    public void insert(GPSPoint point) {
        try {
            indexLock.writeLock().lock();

            // 添加到R树索引
            LocalDate date = point.getTimestamp().toLocalDate();
            RTreeIndex rtree = dailyIndices.computeIfAbsent(date, d -> new RTreeIndex());
            rtree.insert(point);

            // 添加到网格索引
            String gridKey = calculateGridKey(point.getLongitude(), point.getLatitude(), date);
            gridCache.computeIfAbsent(gridKey, k -> ConcurrentHashMap.newKeySet()).add(point);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * 查询指定时空范围内的GPS点，带性能日志
     */
    public List<GPSPoint> query(double minLon, double minLat, double maxLon, double maxLat,
                                LocalDateTime startTime, LocalDateTime endTime) {
        long queryStartTime = System.currentTimeMillis();
        System.out.println("[性能] 开始时空索引查询: 范围[" +
                String.format("%.4f,%.4f to %.4f,%.4f", minLon, minLat, maxLon, maxLat) +
                "], 时间[" + startTime + " to " + endTime + "]");

        try {
            indexLock.readLock().lock();

            // 尝试使用网格索引进行快速查询
            List<GPSPoint> results = tryGridIndexQuery(minLon, minLat, maxLon, maxLat, startTime, endTime);

            // 如果网格索引未命中或结果为空，回退到R树查询
            if (results.isEmpty()) {
                results = rTreeQuery(minLon, minLat, maxLon, maxLat, startTime, endTime);
            }

            long queryEndTime = System.currentTimeMillis();
            System.out.println("[性能] 时空索引查询完成: 找到 " + results.size() +
                    " 个点, 耗时 " + (queryEndTime - queryStartTime) + " 毫秒");

            return results;
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * 使用R树索引进行查询
     */
    private List<GPSPoint> rTreeQuery(double minLon, double minLat, double maxLon, double maxLat,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        long rtreeStartTime = System.currentTimeMillis();
        List<GPSPoint> results = new ArrayList<>();

        // 按日期查询
        LocalDate currentDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        System.out.println("[性能] R树查询: 日期范围 " + currentDate + " 到 " + endDate);

        // 预估结果大小，减少列表扩容
        int estimatedCapacity = estimateResultSize(minLon, minLat, maxLon, maxLat,
                startTime, endTime);
        List<GPSPoint> allPoints = new ArrayList<>(estimatedCapacity);

        while (!currentDate.isAfter(endDate)) {
            RTreeIndex rtree = dailyIndices.get(currentDate);
            if (rtree != null) {
                long dateStartTime = System.currentTimeMillis();
                List<GPSPoint> pointsInDay = rtree.query(minLon, minLat, maxLon, maxLat);
                allPoints.addAll(pointsInDay);
                long dateEndTime = System.currentTimeMillis();

                System.out.println("[性能] R树查询: 日期 " + currentDate +
                        " 找到 " + pointsInDay.size() + " 个点, 耗时 " +
                        (dateEndTime - dateStartTime) + " 毫秒");
            } else {
                System.out.println("[性能] R树查询: 日期 " + currentDate + " 无索引数据");
            }
            currentDate = currentDate.plusDays(1);
        }

        // 时间过滤
        long filterStartTime = System.currentTimeMillis();
        for (GPSPoint point : allPoints) {
            LocalDateTime timestamp = point.getTimestamp();
            if (!timestamp.isBefore(startTime) && !timestamp.isAfter(endTime)) {
                results.add(point);
            }
        }
        long filterEndTime = System.currentTimeMillis();

        System.out.println("[性能] R树查询: 时间过滤 " + allPoints.size() +
                " -> " + results.size() + " 个点, 耗时 " +
                (filterEndTime - filterStartTime) + " 毫秒");

        long rtreeEndTime = System.currentTimeMillis();
        System.out.println("[性能] R树查询: 总耗时 " + (rtreeEndTime - rtreeStartTime) + " 毫秒");

        return results;
    }

    /**
     * 尝试使用网格索引进行快速查询
     */
    private List<GPSPoint> tryGridIndexQuery(double minLon, double minLat, double maxLon, double maxLat,
                                             LocalDateTime startTime, LocalDateTime endTime) {
        long gridStartTime = System.currentTimeMillis();
        List<GPSPoint> results = new ArrayList<>();

        // 计算网格范围
        int minLonGrid = (int)(minLon / GRID_SIZE);
        int minLatGrid = (int)(minLat / GRID_SIZE);
        int maxLonGrid = (int)(maxLon / GRID_SIZE);
        int maxLatGrid = (int)(maxLat / GRID_SIZE);

        // 跨度太大时不使用网格索引
        if ((maxLonGrid - minLonGrid + 1) * (maxLatGrid - minLatGrid + 1) > 25) {
            System.out.println("[性能] 网格索引: 区域跨度过大，不使用网格索引");
            return results;
        }

        LocalDate currentDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        // 日期跨度太大时不使用网格索引
        if (currentDate.until(endDate).getDays() > 3) {
            System.out.println("[性能] 网格索引: 时间跨度过大，不使用网格索引");
            return results;
        }

        int gridHits = 0;
        int totalGrids = 0;

        while (!currentDate.isAfter(endDate)) {
            for (int lonGrid = minLonGrid; lonGrid <= maxLonGrid; lonGrid++) {
                for (int latGrid = minLatGrid; latGrid <= maxLatGrid; latGrid++) {
                    totalGrids++;
                    String gridKey = lonGrid + "_" + latGrid + "_" + currentDate;
                    Set<GPSPoint> pointsInGrid = gridCache.get(gridKey);

                    if (pointsInGrid != null) {
                        gridHits++;
                        for (GPSPoint point : pointsInGrid) {
                            // 精确的边界检查
                            if (point.getLongitude() >= minLon && point.getLongitude() <= maxLon &&
                                    point.getLatitude() >= minLat && point.getLatitude() <= maxLat) {

                                // 时间过滤
                                LocalDateTime timestamp = point.getTimestamp();
                                if (!timestamp.isBefore(startTime) && !timestamp.isAfter(endTime)) {
                                    results.add(point);
                                }
                            }
                        }
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        long gridEndTime = System.currentTimeMillis();
        System.out.println("[性能] 网格索引: 命中 " + gridHits + "/" + totalGrids +
                " 个网格, 找到 " + results.size() + " 个点, 耗时 " +
                (gridEndTime - gridStartTime) + " 毫秒");

        return results;
    }

    /**
     * 计算网格键
     */
    private String calculateGridKey(double longitude, double latitude, LocalDate date) {
        int lonGrid = (int)(longitude / GRID_SIZE);
        int latGrid = (int)(latitude / GRID_SIZE);
        return lonGrid + "_" + latGrid + "_" + date;
    }

    /**
     * 估计结果集大小
     */
    private int estimateResultSize(double minLon, double minLat, double maxLon, double maxLat,
                                   LocalDateTime startTime, LocalDateTime endTime) {
        // 基于区域大小和时间跨度估计
        double areaSize = (maxLon - minLon) * (maxLat - minLat);
        int daySpan = startTime.toLocalDate().until(endTime.toLocalDate()).getDays() + 1;

        // 基础估计: 区域越大，时间跨度越长，预期结果越多
        return Math.max(100, (int)(areaSize * 10000 * daySpan));
    }

    /**
     * 标记日期索引已完成
     */
    public void markIndexComplete(LocalDate date) {
        completedDates.add(date);
    }

    /**
     * 检查日期索引是否已构建
     */
    public boolean isIndexBuilt(LocalDate date) {
        return completedDates.contains(date);
    }

    /**
     * 清除索引
     */
    public void clearIndex() {
        try {
            indexLock.writeLock().lock();
            dailyIndices.clear();
            gridCache.clear();
            completedDates.clear();
        } finally {
            indexLock.writeLock().unlock();
        }
    }
}
