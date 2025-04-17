package com.codex.taxitrajectory.repository;

import com.codex.taxitrajectory.model.core.TaxiRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;

/**
 * 数据加载接口：
 */
public interface TaxiRepository {

    NavigableMap<LocalDateTime, TaxiRecord> getRecordsByTaxiId(String taxiId);

    List<TaxiRecord> getRecordsByTaxiIdAsList(String taxiId);

    List<TaxiRecord> getRecordsByTimeRange(String taxiId, LocalDateTime start, LocalDateTime end);

    Set<String> getAllTaxiIds();

    void refresh(String taxiId); // 强制刷新缓存
}
