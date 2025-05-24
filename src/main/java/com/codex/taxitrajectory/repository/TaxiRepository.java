package com.codex.taxitrajectory.repository;

import com.codex.taxitrajectory.model.core.TaxiRecord;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 出租车轨迹数据仓库接口
 *
 * 定义了访问和管理出租车轨迹数据的核心操作。
 * 实现此接口的类将负责从底层数据源（如文件、数据库或API）获取轨迹信息，并可能包含缓存机制来优化性能。
 */
public interface TaxiRepository {

    /**
     * 根据出租车ID获取其所有轨迹记录。
     *
     * @param taxiId 出租车唯一标识符。
     * @return 一个 {@link NavigableMap}，其中键是轨迹记录的时间戳（{@link LocalDateTime}），
     * 值是对应的 {@link TaxiRecord} 对象。该 Map 会按照时间戳自然排序。
     */
    NavigableMap<LocalDateTime, TaxiRecord> getRecordsByTaxiId(String taxiId);

    /**
     * 根据出租车ID获取其所有轨迹记录的列表形式。
     *
     * @param taxiId 出租车唯一标识符。
     * @return 一个包含所有 {@link TaxiRecord} 对象的 {@link List}，列表中的记录按时间顺序排列。
     */
    List<TaxiRecord> getRecordsByTaxiIdAsList(String taxiId);

    /**
     * 根据出租车ID和指定的时间范围获取轨迹记录。
     *
     * @param taxiId 出租车唯一标识符。
     * @param start 轨迹记录的起始时间（包含）。
     * @param end 轨迹记录的结束时间（包含）。
     * @return 一个包含在指定时间范围内的 {@link TaxiRecord} 对象的 {@link List}。
     * 如果指定范围内没有记录，则返回空列表。
     */
    List<TaxiRecord> getRecordsByTimeRange(String taxiId, LocalDateTime start, LocalDateTime end);

    /**
     * 获取所有可用的出租车ID的集合。
     *
     * @return 一个包含所有唯一出租车ID的 {@link Set}。
     */
    Set<String> getAllTaxiIds();

    /**
     * 以流（Stream）的形式获取特定出租车ID的轨迹记录。
     * 这种方法适用于处理大量数据，因为它允许按需处理记录，而无需一次性将所有数据加载到内存中。
     *
     * @param taxiId 出租车唯一标识符。
     * @return 一个包含 {@link TaxiRecord} 对象的 {@link Stream}。
     * @throws IOException 如果在访问或解析底层数据源时发生I/O错误。
     */
    Stream<TaxiRecord> streamRecordsByTaxiId(String taxiId) throws IOException;

    /**
     * 强制刷新指定出租车ID的缓存数据。
     * 调用此方法将使缓存中与该出租车ID相关的旧数据失效，以便下次请求时从原始数据源重新加载最新数据。
     *
     * @param taxiId 需要刷新缓存的出租车ID。
     **/
    void refresh(String taxiId);
}
