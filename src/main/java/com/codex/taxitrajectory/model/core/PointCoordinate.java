package com.codex.taxitrajectory.model.core;

import lombok.Data;

/**
 * 类PointCoordinate将用于表示单个地理坐标点。
 */
@Data
public class PointCoordinate {
    private double longitude; // 经度
    private double latitude;  // 纬度


    public PointCoordinate(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
}