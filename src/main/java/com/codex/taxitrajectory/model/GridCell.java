package com.codex.taxitrajectory.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 网格单元类，表示地图上的一个网格单元格
 */
@Data
public class GridCell {
    private int row;
    private int col;
    private double minLon; // 左边界经度
    private double minLat; // 下边界纬度
    private double maxLon; // 右边界经度
    private double maxLat; // 上边界纬度
    private Map<LocalDateTime, Set<String>> timeSlotTaxiIds;  // 按时间段存储经过的出租车ID

    public GridCell(int row, int col, double minLon, double minLat, double maxLon, double maxLat) {
        this.row = row;
        this.col = col;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.timeSlotTaxiIds = new HashMap<>();
    }


    // 判断一个GPS点是否在该网格内
    public boolean contains(GPSPoint point) {
        return point.isInRegion(minLon, minLat, maxLon, maxLat);
    }

    /**
     * 添加车辆记录
     */
    public void addTaxi(String taxiId, LocalDateTime timestamp) {
        // 按小时取整处理时间戳
        LocalDateTime timeSlot = timestamp.withMinute(0).withSecond(0).withNano(0);

        timeSlotTaxiIds.computeIfAbsent(timeSlot, k -> new HashSet<>())
                .add(taxiId);
    }

    /**
     * 获取特定时间段的车流密度
     */
    public int getDensity(LocalDateTime timeSlot) {
        return timeSlotTaxiIds.getOrDefault(timeSlot, Collections.emptySet()).size();
    }
}
