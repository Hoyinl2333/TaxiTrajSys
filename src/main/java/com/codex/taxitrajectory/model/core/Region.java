package com.codex.taxitrajectory.model.core;

import lombok.Data;

// 定义一个矩形地理区域
@Data
public class Region {
    private double minLat; // 最小纬度
    private double maxLat; // 最大纬度
    private double minLon; // 最小经度
    private double maxLon; // 最大经度

    public Region(double minLon,double minLat,double maxLon,double maxLat) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }
}