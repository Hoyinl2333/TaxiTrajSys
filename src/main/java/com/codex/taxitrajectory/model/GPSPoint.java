package com.codex.taxitrajectory.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * GPS坐标点类，包含经纬度、时间戳和出租车ID
 */
@Data
public class GPSPoint {
    private double longitude; // 经度
    private double latitude; // 纬度
    private LocalDateTime timestamp;  // 时间戳
    private String taxiId;     // 出租车ID

    // 疑似GPSPoint作用和TaxiRecord作用有点重合


    public GPSPoint(double longitude, double latitude, LocalDateTime timestamp, String taxiId) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
        this.taxiId = taxiId;
    }


    /**
     * 从 TaxiRecord 创建 GPSPoint
     * @param record 出租车轨迹记录
     * @return GPSPoint 对象
     */
    public static GPSPoint fromTaxiRecord(TaxiRecord record) {
        return new GPSPoint(record.getLongitude(), record.getLatitude(),record.getTimestamp(),record.getTaxiId());
    }


    // 判断点是否在矩形区域内
    public boolean isInRegion(double minLon, double minLat, double maxLon, double maxLat) {
        return longitude >= minLon && longitude <= maxLon
                && latitude >= minLat && latitude <= maxLat;
    }
}
