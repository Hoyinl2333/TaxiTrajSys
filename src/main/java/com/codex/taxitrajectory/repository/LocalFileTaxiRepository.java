package com.codex.taxitrajectory.repository;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.cache.InMemoryTaxiCache;
import com.codex.taxitrajectory.repository.data.FileTaxiDataSource; // 可能仍需用于 listAllTaxiIds
import com.codex.taxitrajectory.repository.parser.TaxiRecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * 本地文件出租车轨迹数据仓库实现类。
 * 负责从本地文件系统获取出租车轨迹数据，并利用内存缓存优化数据访问。
 */
@Repository
public class LocalFileTaxiRepository implements TaxiRepository {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileTaxiRepository.class);

    // 控制是否启用仓库层的日志记录，通过 Spring 配置注入
    @Value("${logging.repository.enabled:true}")
    private boolean loggingEnabled;

    // 内存缓存，用于存储和快速检索出租车轨迹数据
    private final InMemoryTaxiCache cache;
    // 文件数据源，负责从文件系统中加载原始轨迹数据
    private final FileTaxiDataSource dataSource;
    // 轨迹记录解析器，用于将原始数据解析成 TaxiRecord 对象
    private final TaxiRecordParser parser;

    // 缓存的出租车ID集合，使用 volatile 关键字保证多线程环境下的可见性
    // 避免因为指令重排导致其他线程读取到旧的或不完整的数据
    private volatile Set<String> cachedTaxiIds = null;


    /**
     * 构造函数，通过依赖注入初始化缓存、数据源和解析器。
     * @param cache 内存缓存实例
     * @param dataSource 文件数据源实例
     * @param parser 轨迹记录解析器实例
     */
    public LocalFileTaxiRepository(InMemoryTaxiCache cache, FileTaxiDataSource dataSource,TaxiRecordParser parser) {
        this.cache = cache;
        this.dataSource = dataSource;
        this.parser = parser;
    }


    /**
     * 根据出租车ID获取其所有轨迹记录。
     * 优先从缓存中获取数据，如果缓存中没有，则由缓存内部机制负责加载。
     * @param taxiId 出租车ID
     * @return 包含轨迹记录的 NavigableMap，按时间排序
     */
    @Override
    public NavigableMap<LocalDateTime, TaxiRecord> getRecordsByTaxiId(String taxiId) {
        // 直接从缓存获取，加载逻辑由缓存内部处理
        return cache.get(taxiId);
    }

    /**
     * 根据出租车ID获取其所有轨迹记录的列表形式，按时间排序。
     * @param taxiId 出租车ID
     * @return 包含轨迹记录的 List
     */
    @Override
    public List<TaxiRecord> getRecordsByTaxiIdAsList(String taxiId) {
        NavigableMap<LocalDateTime, TaxiRecord> records = getRecordsByTaxiId(taxiId);
        // 将 NavigableMap 的值（TaxiRecord）转换为 List
        return new ArrayList<>(records.values());
    }

    /**
     * 根据出租车ID和时间范围获取轨迹记录。
     * @param taxiId 出租车ID
     * @param start 起始时间（包含）
     * @param end 结束时间（包含）
     * @return 匹配时间范围的轨迹记录列表，如果时间范围无效则返回空列表
     */
    @Override
    public List<TaxiRecord> getRecordsByTimeRange(String taxiId, LocalDateTime start, LocalDateTime end) {
        NavigableMap<LocalDateTime, TaxiRecord> records = getRecordsByTaxiId(taxiId);
        // 如果没有记录，则返回空列表
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用 subMap 进行时间范围过滤
        try {
            // subMap(fromKey, fromInclusive, toKey, toInclusive) 用于获取子映射
            return new ArrayList<>(records.subMap(start, true, end, true).values());
        } catch (IllegalArgumentException e) {
            // 捕获非法时间范围异常，并根据日志配置打印警告信息
            if (loggingEnabled) {
                logger.warn("为出租车ID {} 提供了无效的时间范围：开始时间={}, 结束时间={}. {}", taxiId, start, end, e.getMessage());
            }
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有唯一的出租车ID。
     * 该方法实现了双重检查锁定（Double-Checked Locking）以确保在多线程环境下高效且线程安全地加载ID列表，
     * 只在第一次访问时从数据源加载。
     * @return 包含所有出租车ID的 Set
     */
    @Override
    public Set<String> getAllTaxiIds() {
        // 第一次检查：如果 cachedTaxiIds 已经初始化，则直接返回，避免同步开销
        if (cachedTaxiIds == null) {
            // 进入同步块，确保只有一个线程能够执行初始化逻辑
            synchronized (this) {
                // 第二次检查：在同步块内部再次检查，防止多线程同时通过第一次检查后重复初始化
                if (cachedTaxiIds == null) {
                    if(loggingEnabled) {
                        logger.info("正在从数据源加载所有出租车ID。");
                    }
                    // 从数据源加载所有出租车ID
                    cachedTaxiIds = dataSource.listAllTaxiIds();
                }
            }
        }
        return cachedTaxiIds;
    }

    /**
     * 以流（Stream）的形式获取特定出租车ID的轨迹记录。
     * 这种方式允许对大量数据进行高效处理，而无需一次性加载所有数据到内存。
     * 直接从文件数据源加载资源并使用解析器进行流式解析。
     * @param taxiId 出租车ID
     * @return 包含轨迹记录的 Stream
     * @throws IOException 如果在加载或解析文件时发生I/O错误
     */
    @Override
    public Stream<TaxiRecord> streamRecordsByTaxiId(String taxiId) throws IOException {
        if (loggingEnabled) {
            logger.debug("正在直接从源流式传输出租车ID {} 的记录。", taxiId);
        }
        // 从数据源加载对应的文件资源
        Resource resource = dataSource.load(taxiId);

        // 检查资源是否存在或有效
        if (resource == null || !resource.exists()) {
            if (loggingEnabled) {
                logger.warn("未找到出租车ID {} 的轨迹文件，返回空流。", taxiId);
            }
            return Stream.empty(); // 如果文件不存在，返回空流
        }

        try {
            // 直接调用解析器的流式方法来解析文件内容
            return parser.parseAsStream(resource);
        } catch (IOException e) {
            // 捕获并记录I/O错误
            if (loggingEnabled) {
                logger.error("流式传输出租车ID {} 的轨迹文件时发生错误。", taxiId, e);
            }
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 刷新特定出租车ID的缓存数据。
     * 当外部数据源的数据发生变化时，可以调用此方法使缓存中的旧数据失效，以便下次请求时重新加载最新数据。
     * @param taxiId 需要刷新的出租车ID
     */
    @Override
    public void refresh(String taxiId) {
        // 调用缓存的失效方法，将该出租车ID的记录从缓存中移除
        cache.evict(taxiId);
    }
}