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

@Repository
public class LocalFileTaxiRepository implements TaxiRepository {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileTaxiRepository.class);
    @Value("${logging.repository.enabled:true}")
    private boolean loggingEnabled;

    private final InMemoryTaxiCache cache;
    private final FileTaxiDataSource dataSource; // listAllTaxiIds 仍需要
    private final TaxiRecordParser parser;
    private volatile Set<String> cachedTaxiIds = null; // 出租车 ID 列表，使用 volatile 保证可见性


    public LocalFileTaxiRepository(InMemoryTaxiCache cache, FileTaxiDataSource dataSource,TaxiRecordParser parser) {
        this.cache = cache;
        this.dataSource = dataSource;
        this.parser = parser;
    }


    @Override
    public NavigableMap<LocalDateTime, TaxiRecord> getRecordsByTaxiId(String taxiId) {
        // 直接从缓存获取，加载逻辑由缓存内部处理
        return cache.get(taxiId);
    }

    // 返回 List 格式的轨迹记录（按时间排序）
    @Override
    public List<TaxiRecord> getRecordsByTaxiIdAsList(String taxiId) {
        NavigableMap<LocalDateTime, TaxiRecord> records = getRecordsByTaxiId(taxiId);
        return new ArrayList<>(records.values());
    }

    @Override
    public List<TaxiRecord> getRecordsByTimeRange(String taxiId, LocalDateTime start, LocalDateTime end) {
        NavigableMap<LocalDateTime, TaxiRecord> records = getRecordsByTaxiId(taxiId);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用 subMap 进行时间范围过滤
        try {
            return new ArrayList<>(records.subMap(start, true, end, true).values());
        } catch (IllegalArgumentException e) {
            if (loggingEnabled) {
                logger.warn("Invalid time range provided for taxiId {}: start={}, end={}. {}", taxiId, start, end, e.getMessage());
            }
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getAllTaxiIds() {
        if (cachedTaxiIds == null) {
            synchronized (this) { // 双重检查锁定确保线程安全和效率
                if (cachedTaxiIds == null) {
                    if(loggingEnabled) {
                         logger.info("Loading all taxi IDs from data source.");
                    }
                    cachedTaxiIds = dataSource.listAllTaxiIds();
                    // 可以考虑返回不可变集合
                    // cachedTaxiIds = Collections.unmodifiableSet(dataSource.listAllTaxiIds());
                }
            }
        }
        return cachedTaxiIds;
    }

    // 实现流式方法
    @Override
    public Stream<TaxiRecord> streamRecordsByTaxiId(String taxiId)throws   IOException {
        if (loggingEnabled) {
            logger.debug("Streaming records for taxiId: {} directly from source.", taxiId);
        }
        Resource resource = dataSource.load(taxiId);

        if (resource == null || !resource.exists()) {
            if (loggingEnabled) {
                logger.warn("Trajectory file not found for streaming for taxiId: {}. Returning empty stream.", taxiId);
            }
            return Stream.empty();
        }

        try {
            // 直接调用解析器的流式方法
            return parser.parseAsStream(resource);
        } catch (IOException e) {
            if (loggingEnabled) {
                logger.error("Error streaming trajectory file for taxiId: {}.", taxiId, e);
            }
            throw e;
        }
    }

    @Override
    public void refresh(String taxiId) {
        // 调用缓存的失效方法
        cache.evict(taxiId);
    }
}