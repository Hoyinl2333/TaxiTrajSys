package com.codex.taxitrajectory.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网格管理类 - 负责地图网格划分和网格单元管理
 */
@Data
public class Grid {
    private GridCell[][] cells;
    private double gridSize;  // 网格大小，单位：km
    private double minLon, minLat;  // 地图左下角经纬度
    private double maxLon, maxLat;  // 地图右上角经纬度
    private int rows, cols;  // 网格行数和列数

    // 坐标查找缓存
    private ConcurrentHashMap<String, GridCell> cellLookupCache = new ConcurrentHashMap<>();

    public Grid(double gridSize, double minLon, double minLat, double maxLon, double maxLat) {
        this.gridSize = gridSize;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;

        // 计算网格维度
        double lonStep = gridSizeInLongitude(gridSize);
        double latStep = gridSizeInLatitude(gridSize);
        this.cols = (int) Math.ceil((maxLon - minLon) / lonStep);
        this.rows = (int) Math.ceil((maxLat - minLat) / latStep);

        // 初始化网格单元
        cells = new GridCell[rows][cols];
        initCells();
    }

    /**
     * 初始化所有网格单元
     */
    private void initCells() {
        double lonStep = gridSizeInLongitude(gridSize);
        double latStep = gridSizeInLatitude(gridSize);

        for (int r = 0; r < rows; r++) {
            double cellMinLat = minLat + r * latStep;
            double cellMaxLat = Math.min(cellMinLat + latStep, maxLat);

            for (int c = 0; c < cols; c++) {
                double cellMinLon = minLon + c * lonStep;
                double cellMaxLon = Math.min(cellMinLon + lonStep, maxLon);

                cells[r][c] = new GridCell(r, c, cellMinLon, cellMinLat, cellMaxLon, cellMaxLat);
            }
        }
    }

    /**
     * 根据经纬度获取网格单元 - 使用缓存提高性能
     */
    public GridCell getCellByPosition(double lon, double lat) {
        if (lon < minLon || lon > maxLon || lat < minLat || lat > maxLat) {
            return null;
        }

        // 创建缓存键，降低精度以提高缓存命中率
        String cacheKey = String.format("%.5f:%.5f", lon, lat);

        return cellLookupCache.computeIfAbsent(cacheKey, k -> {
            int col = (int) ((lon - minLon) / gridSizeInLongitude(gridSize));
            int row = (int) ((lat - minLat) / gridSizeInLatitude(gridSize));

            // 边界检查
            if (col >= cols) col = cols - 1;
            if (row >= rows) row = rows - 1;

            return cells[row][col];
        });
    }

    /**
     * 获取所有网格单元
     */
    public List<GridCell> getAllCells() {
        List<GridCell> list = new ArrayList<>(rows * cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                list.add(cells[r][c]);
            }
        }
        return list;
    }

    /**
     * 将km转换为经度差（北京地区约85.3公里/度）
     */
    private double gridSizeInLongitude(double km) {
        return km / 85.3;
    }

    /**
     * 将km转换为纬度差（约111公里/度）
     */
    private double gridSizeInLatitude(double km) {
        return km / 111.0;
    }
}
