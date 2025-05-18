package com.codex.taxitrajectory.model;

import com.codex.taxitrajectory.utils.GeoUtils;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 网格类，管理整个地图的网格划分
 */
@Data
public class Grid {
    private GridCell[][] cells;
    private double gridSize;  // 网格大小(r)，单位：公里
    private double minLon, minLat;  // 整个区域的最小经纬度
    private double maxLon, maxLat;  // 整个区域的最大经纬度
    private int rows, cols;  // 网格行列数

    /**
     * 构造函数，初始化网格
     */
    public Grid(double gridSize, double minLon, double minLat, double maxLon, double maxLat) {
        this.gridSize = gridSize;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;

        // 计算网格行列数（经度方向1度约等于85.3公里，纬度方向1度约等于111公里）
        double lonDiff = maxLon - minLon;
        double latDiff = maxLat - minLat;
        this.cols = (int) Math.ceil(lonDiff / gridSizeInLongitude(gridSize));
        this.rows = (int) Math.ceil(latDiff / gridSizeInLatitude(gridSize));

        // 初始化网格单元
        initializeCells();
    }

    /**
     * 初始化所有网格单元
     */
    private void initializeCells() {
        cells = new GridCell[rows][cols];
        double lonStep = gridSizeInLongitude(gridSize);
        double latStep = gridSizeInLatitude(gridSize);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double cellMinLon = minLon + j * lonStep;
                double cellMinLat = minLat + i * latStep;
                double cellMaxLon = Math.min(minLon + (j + 1) * lonStep, maxLon);
                double cellMaxLat = Math.min(minLat + (i + 1) * latStep, maxLat);

                cells[i][j] = new GridCell(i, j, cellMinLon, cellMinLat, cellMaxLon, cellMaxLat);
            }
        }
    }

    /**
     * 根据经纬度定位到对应的网格
     */
    public GridCell getCellByPosition(double longitude, double latitude) {
        if (longitude < minLon || longitude > maxLon || latitude < minLat || latitude > maxLat) {
            return null;
        }

        int col = (int) ((longitude - minLon) / gridSizeInLongitude(gridSize));
        int row = (int) ((latitude - minLat) / gridSizeInLatitude(gridSize));

        // 边界处理
        if (col >= cols) col = cols - 1;
        if (row >= rows) row = rows - 1;

        return cells[row][col];
    }

    /**
     * 获取所有单元格
     */
    public List<GridCell> getAllCells() {
        List<GridCell> allCells = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                allCells.add(cells[i][j]);
            }
        }
        return allCells;
    }

    /**
     * 将公里距离转换为经度差
     */
    private double gridSizeInLongitude(double kmSize) {
        // 北京地区1度经度约等于85.3公里
        return kmSize / 85.3;
    }

    /**
     * 将公里距离转换为纬度差
     */
    private double gridSizeInLatitude(double kmSize) {
        // 北京地区1度纬度约等于111公里
        return kmSize / 111.0;
    }

}
