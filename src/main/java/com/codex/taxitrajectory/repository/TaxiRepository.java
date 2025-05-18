package com.codex.taxitrajectory.repository;

import com.codex.taxitrajectory.model.core.TaxiRecord;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 数据加载接口：
 */
public interface TaxiRepository {

    NavigableMap<LocalDateTime, TaxiRecord> getRecordsByTaxiId(String taxiId);

    List<TaxiRecord> getRecordsByTaxiIdAsList(String taxiId);

    List<TaxiRecord> getRecordsByTimeRange(String taxiId, LocalDateTime start, LocalDateTime end);

    Set<String> getAllTaxiIds();

    // 新增的流式方法加载某个id的数据，避免瞬间加载过多  TODO: 拓展更多使用场景，考虑加入时间范围过滤
    Stream<TaxiRecord> streamRecordsByTaxiId(String taxiId) throws IOException; // 声明 IOException


    void refresh(String taxiId); // 强制刷新缓存
}
