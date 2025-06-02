package com.codex.taxitrajectory.repository.cache;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.data.FileTaxiDataSource;
import com.codex.taxitrajectory.repository.parser.TaxiRecordParser;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * InMemoryTaxiCache 是一个基于 Caffeine 实现的出租车轨迹缓存组件。
 * 其作用是根据出租车ID缓存轨迹数据，以避免重复从文件加载并解析，提高系统性能。
 */
@Component
public class InMemoryTaxiCache {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryTaxiCache.class);

    // 是否开启日志 (从配置文件注入，默认 true)
    @Value("${logging.repository.enabled:true}")
    private boolean loggingEnabled; 

    // 本地数据源，用于从文件加载轨迹数据
    private final FileTaxiDataSource dataSource;

    // 用于解析轨迹文件的解析器
    private final TaxiRecordParser parser;

    // 缓存最大条目数（从配置文件中注入，默认 5000）
    @Value("${cache.taxi.maxSize:5000}")
    private long maxSize;

    // 缓存条目在最后访问后多长时间过期（单位：分钟，默认 60）
    @Value("${cache.taxi.expireAfterAccessMinutes:60}")
    private long expireAfterAccessMinutes;

    // Caffeine 的 LoadingCache 实例，key 为出租车 ID，value 为时间排序的轨迹记录
    private LoadingCache<String, NavigableMap<LocalDateTime, TaxiRecord>> cache;

    // 构造方法注入数据源与解析器
    public InMemoryTaxiCache(FileTaxiDataSource dataSource, TaxiRecordParser parser) {
        this.dataSource = dataSource;
        this.parser = parser;
    }

    /**
     * 容器初始化后调用，构建 Caffeine 缓存实例。
     * 使用 @PostConstruct 确保在 Spring 完成依赖注入后执行。
     */
    @PostConstruct
    public void initializeCache() {
        if (loggingEnabled) {
            logger.info("Initializing Caffeine cache with maxSize={}, expireAfterAccess={}min", maxSize,
                    expireAfterAccessMinutes);
        }

        // 构建缓存实例
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize) // 设置最大容量
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES) // 访问后过期时间
                .recordStats() // 开启统计信息（可用于性能分析）
                .removalListener((String key, NavigableMap<LocalDateTime, TaxiRecord> value,
                        RemovalCause cause) -> {
                    if (loggingEnabled) {
                        logger.info("Cache entry removed. Key: {}, Cause: {}", key, cause);
                    }
                })
                .build(this::loadTaxiData); // 缓存加载逻辑
    }

    /**
     * 当缓存 miss 时调用的方法，用于加载出租车轨迹数据。
     * @param taxiId 出租车唯一标识符
     * @return 轨迹数据（按时间排序的 Map）
     */
    private NavigableMap<LocalDateTime, TaxiRecord> loadTaxiData(String taxiId) throws Exception {
        if (loggingEnabled) {
            logger.debug("Cache miss. Loading data for taxiId: {}", taxiId);
        }
        Resource resource = dataSource.load(taxiId);

        // 若资源不存在，返回空轨迹集合
        if (resource == null || !resource.exists()) {
            if (loggingEnabled) {
                logger.warn("Trajectory file not found during cache load for taxiId: {}. Returning empty map.", taxiId);
            }
            return new TreeMap<>();
        }

        // 解析轨迹文件
        try {
            return parser.parse(resource);
        } catch (Exception e) {
            if (loggingEnabled) {
                logger.error("Error parsing trajectory file for taxiId: {} during cache load.", taxiId, e);
            }
            return new TreeMap<>();
        }
    }

    /**
     * 查询某辆出租车的轨迹数据，若缓存中无则自动加载。
     * @param taxiId 出租车ID
     * @return 时间排序的轨迹数据 Map
     */
    public NavigableMap<LocalDateTime, TaxiRecord> get(String taxiId) {
        try {
            return cache.get(taxiId);
        } catch (Exception e) {
            // 检查日志开关
            if (loggingEnabled) {
                logger.error("Exception occurred while getting data from cache for taxiId: {}", taxiId, e);
            }
            // 即使日志关闭，也应返回空对象
            return new TreeMap<>();
        }
    }

    /**
     * 主动移除某辆出租车的缓存数据。
     * @param taxiId 出租车ID
     */
    public void evict(String taxiId) {
        // 检查日志开关
        if (loggingEnabled) {
            logger.info("Evicting cache for taxiId: {}", taxiId);
        }
        cache.invalidate(taxiId);
    }

    /**
     * 清空整个缓存。
     */
    public void clear() {
        if (loggingEnabled) {
            logger.info("Clearing all cache entries.");
        }
        cache.invalidateAll();
    }

    /**
     * 获取缓存统计信息，如命中率、加载次数、加载失败等。
     * @return Caffeine 缓存统计信息对象
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getStats() {
        // 获取统计信息通常不涉及敏感操作，可以不加日志开关，或者根据需要添加
        return cache.stats();
    }
}