package com.codex.taxitrajectory.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网格单元类 - 使用BitSet优化内存占用
 */
@Data
public class GridCell {
    private int row;  // 行号
    private int col;  // 列号
    private double minLon;  // 左边界经度
    private double minLat;  // 下边界纬度
    private double maxLon;  // 右边界经度
    private double maxLat;  // 上边界纬度

    // 使用ConcurrentHashMap保证线程安全
    private Map<LocalDateTime, BitSet> timeSlotTaxiIds = new ConcurrentHashMap<>();
    // 出租车ID到索引的映射
    private Map<String, Integer> taxiIdToIndex = new ConcurrentHashMap<>();
    // 索引到出租车ID的反向映射
    private Map<Integer, String> indexToTaxiId = new ConcurrentHashMap<>();
    // 下一个可用的出租车ID索引
    private int nextTaxiIdIndex = 0;

    public GridCell(int row, int col, double minLon, double minLat, double maxLon, double maxLat) {
        this.row = row;
        this.col = col;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
    }

    /**
     * 判断点是否在网格内
     */
    public boolean contains(double lon, double lat) {
        return lon >= minLon && lon <= maxLon && lat >= minLat && lat <= maxLat;
    }

    /**
     * 添加出租车到指定时间槽 - 使用BitSet优化内存
     */
    public void addTaxi(String taxiId, LocalDateTime timeSlot) {
        // 获取或创建该出租车ID的索引
        Integer taxiIndex = taxiIdToIndex.computeIfAbsent(taxiId, id -> {
            int index = nextTaxiIdIndex++;
            indexToTaxiId.put(index, id);
            return index;
        });

        // 获取或创建该时间槽的BitSet
        BitSet bitSet = timeSlotTaxiIds.computeIfAbsent(timeSlot, ts -> new BitSet());

        // 设置该出租车的位
        bitSet.set(taxiIndex);
    }

    /**
     * 获取指定时间槽的车流密度（不同出租车数量）
     */
    public int getDensity(LocalDateTime timeSlot) {
        BitSet bitSet = timeSlotTaxiIds.get(timeSlot);
        return bitSet == null ? 0 : bitSet.cardinality();
    }

    /**
     * 获取指定时间槽的所有出租车ID（用于测试）
     */
    public Set<String> getTaxiIds(LocalDateTime timeSlot) {
        BitSet bitSet = timeSlotTaxiIds.get(timeSlot);
        if (bitSet == null) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            result.add(indexToTaxiId.get(i));
        }
        return result;
    }
}