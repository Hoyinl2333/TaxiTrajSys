package com.codex.taxitrajectory.utils;

import com.codex.taxitrajectory.model.GPSPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 时空分区索引 - 按天构建空间索引，提高查询性能
 */
@Component
public class TimePartitionedSpatialIndex {

    // 按天索引的空间树
    private final Map<LocalDate, STRtree> timePartitionedIndex = new ConcurrentHashMap<>();

    // 索引构建锁（按天）- 防止并发构建同一天的索引
    private final Map<LocalDate, ReentrantLock> indexBuildLocks = new ConcurrentHashMap<>();

    // 索引完成标记 - 标记哪些天的数据已完成索引
    private final Map<LocalDate, Boolean> indexCompleted = new ConcurrentHashMap<>();

    // 查询结果缓存 - 缓存热门查询
    private final Map<QueryCacheKey, List<GPSPoint>> queryResultCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100; // 缓存大小限制

    /**
     * 插入GPS点到时空分区索引
     */
    public void insert(GPSPoint point) {
        LocalDate day = point.getTimestamp().toLocalDate();

        // 获取该天的空间索引，如果不存在则创建
        STRtree dayIndex = timePartitionedIndex.computeIfAbsent(day, d -> new STRtree());

        // 创建空间包络并插入到索引
        Coordinate coord = new Coordinate(point.getLongitude(), point.getLatitude());
        Envelope envelope = new Envelope(coord);
        dayIndex.insert(envelope, point);
    }

    /**
     * 按需获取或构建指定日期的空间索引
     */
    public STRtree getOrBuildIndex(LocalDate day) {
        // 如果索引已完成构建，直接返回
        if (Boolean.TRUE.equals(indexCompleted.get(day))) {
            return timePartitionedIndex.get(day);
        }

        // 获取该天的构建锁，避免并发构建
        ReentrantLock lock = indexBuildLocks.computeIfAbsent(day, d -> new ReentrantLock());

        if (lock.tryLock()) {
            try {
                // 双重检查，避免锁竞争后重复构建
                if (Boolean.TRUE.equals(indexCompleted.get(day))) {
                    return timePartitionedIndex.get(day);
                }

                // 如果索引不存在，初始化一个
                STRtree index = timePartitionedIndex.computeIfAbsent(day, d -> new STRtree());


                index.build();

                // 标记索引完成
                indexCompleted.put(day, Boolean.TRUE);
                return index;
            } finally {
                lock.unlock();
            }
        } else {
            // 如果无法获取锁，说明有其他线程正在构建，等待后返回
            try {
                Thread.sleep(100); // 短暂等待
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getOrBuildIndex(day); // 递归尝试
        }
    }

    /**
     * 区域范围查询 - 先按时间过滤，再做空间查询
     */
    @SuppressWarnings("unchecked")
    public List<GPSPoint> query(double minLon, double minLat, double maxLon, double maxLat,
                                LocalDateTime startTime, LocalDateTime endTime) {
        // 构建缓存键
        QueryCacheKey cacheKey = new QueryCacheKey(minLon, minLat, maxLon, maxLat, startTime, endTime);

        // 检查缓存
        if (queryResultCache.containsKey(cacheKey)) {
            return queryResultCache.get(cacheKey);
        }

        List<GPSPoint> results = new ArrayList<>();

        // 确定查询涉及的天数
        LocalDate currentDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        // 创建空间查询范围
        Envelope queryEnvelope = new Envelope(minLon, maxLon, minLat, maxLat);

        // 按天查询
        while (!currentDate.isAfter(endDate)) {
            // 检查该天的索引是否存在
            STRtree dayIndex = getOrBuildIndex(currentDate);

            // TODO: 这里查询有BUG
            if (dayIndex != null) {
                // 执行空间查询
                List<Object> dayResults = dayIndex.query(queryEnvelope);

                // 处理查询结果，过滤时间范围
                for (Object obj : dayResults) {
                    if (obj instanceof GPSPoint) {
                        GPSPoint point = (GPSPoint) obj;
                        LocalDateTime timestamp = point.getTimestamp();

                        // 时间范围过滤
                        if (!timestamp.isBefore(startTime) && !timestamp.isAfter(endTime)) {
                            results.add(point);
                        }
                    }
                }
            }

            // 前进到下一天
            currentDate = currentDate.plusDays(1);
        }

        // 缓存查询结果（简单的LRU缓存实现）
        if (queryResultCache.size() >= MAX_CACHE_SIZE) {
            // 如果缓存已满，移除一个随机条目（更好的实现应使用真正的LRU策略）
            queryResultCache.remove(queryResultCache.keySet().iterator().next());
        }
        queryResultCache.put(cacheKey, results);

        return results;
    }

    /**
     * 标记某天的索引已完成
     */
    public void markIndexComplete(LocalDate day) {
        indexCompleted.put(day, Boolean.TRUE);
    }

    /**
     * 查询缓存键类
     */
    private static class QueryCacheKey {
        private final double minLon;
        private final double minLat;
        private final double maxLon;
        private final double maxLat;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public QueryCacheKey(double minLon, double minLat, double maxLon, double maxLat,
                             LocalDateTime startTime, LocalDateTime endTime) {
            this.minLon = minLon;
            this.minLat = minLat;
            this.maxLon = maxLon;
            this.maxLat = maxLat;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueryCacheKey that = (QueryCacheKey) o;
            return Double.compare(that.minLon, minLon) == 0 &&
                    Double.compare(that.minLat, minLat) == 0 &&
                    Double.compare(that.maxLon, maxLon) == 0 &&
                    Double.compare(that.maxLat, maxLat) == 0 &&
                    startTime.equals(that.startTime) &&
                    endTime.equals(that.endTime);
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + Double.hashCode(minLon);
            result = 31 * result + Double.hashCode(minLat);
            result = 31 * result + Double.hashCode(maxLon);
            result = 31 * result + Double.hashCode(maxLat);
            result = 31 * result + startTime.hashCode();
            result = 31 * result + endTime.hashCode();
            return result;
        }
    }
}
