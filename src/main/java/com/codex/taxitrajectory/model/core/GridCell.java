package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 简化后的网格单元类，仅包含空间信息和标识符.
 * 主要用于路径分析 (F7/F8) 或其他基于网格位置的功能.
 */
@Data // 提供 getter, setter, toString, equals, hashCode
@EqualsAndHashCode(of = {"row", "col"}) // 确保 equals/hashCode 基于 row 和 col
public class GridCell {

    // 核心标识符
    private final int row;
    private final int col;

    // 地理边界
    private final double minLon;
    private final double minLat;
    private final double maxLon;
    private final double maxLat;

    // 中心点坐标 (F8 需要)
    @Getter private final double centerLon;
    @Getter private final double centerLat;

    /**
     * 构造函数.
     * @param row 行索引.
     * @param col 列索引.
     * @param minLon 最小经度.
     * @param minLat 最小纬度.
     * @param maxLon 最大经度.
     * @param maxLat 最大纬度.
     */
    public GridCell(int row, int col, double minLon, double minLat, double maxLon, double maxLat) {
        this.row = row;
        this.col = col;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        // 计算中心点
        this.centerLon = minLon + (maxLon - minLon) / 2.0;
        this.centerLat = minLat + (maxLat - minLat) / 2.0;
    }
}