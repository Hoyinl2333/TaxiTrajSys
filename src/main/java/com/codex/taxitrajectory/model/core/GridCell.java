package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表示地图上的一个网格单元。
 * <p>
 * 此类封装了网格单元的行列索引、地理边界（最小/最大经纬度）以及中心点坐标。
 * 网格单元在创建后其属性不可变。
 * 通过行号和列号 (row, col) 来唯一标识和比较相等性。
 * </p>
 */
@Data
@EqualsAndHashCode(of = {"row", "col"})
public class GridCell {

    /**
     * 网格单元的行索引 (0-based)。
     */
    private final int row;
    /**
     * 网格单元的列索引 (0-based)。
     */
    private final int col;

    /**
     * 网格单元覆盖范围的最小经度。
     */
    private final double minLon;
    /**
     * 网格单元覆盖范围的最小纬度。
     */
    private final double minLat;
    /**
     * 网格单元覆盖范围的最大经度。
     */
    private final double maxLon;
    /**
     * 网格单元覆盖范围的最大纬度。
     */
    private final double maxLat;

    /**
     * 网格单元中心点的经度。
     * 在构造时根据边界计算。
     */
    private final double centerLon;
    /**
     * 网格单元中心点的纬度。
     * 在构造时根据边界计算。
     */
    private final double centerLat;

    /**
     * 构造一个新的 GridCell 对象。
     * 中心点坐标 (centerLon, centerLat) 会根据传入的边界自动计算。
     *
     * @param row    单元格的行索引。
     * @param col    单元格的列索引。
     * @param minLon 单元格的最小经度。
     * @param minLat 单元格的最小纬度。
     * @param maxLon 单元格的最大经度。
     * @param maxLat 单元格的最大纬度。
     */
    public GridCell(int row, int col, double minLon, double minLat, double maxLon, double maxLat) {
        this.row = row;
        this.col = col;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        // 在构造时计算并存储中心点坐标
        this.centerLon = minLon + (maxLon - minLon) / 2.0;
        this.centerLat = minLat + (maxLat - minLat) / 2.0;
    }
}