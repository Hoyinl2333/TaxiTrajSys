package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.Getter; // Import Getter
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网格管理类 - 负责地图网格划分和网格单元管理
 */
@Data
public class Grid {
    private static final Logger logger = LoggerFactory.getLogger(Grid.class); // Add logger

    private final GridCell[][] cells; // Mark as final
    private final double gridSize;  // 网格大小，单位：km (final)
    private final double minLon, minLat;  // 地图左下角经纬度 (final)
    private final double maxLon, maxLat;  // 地图右上角经纬度 (final)
    private final int rows;  // 网格行数
    private final int cols;  // 网格列数

    // 坐标查找缓存 (final reference)
    private final ConcurrentHashMap<String, GridCell> cellLookupCache = new ConcurrentHashMap<>();

    public Grid(double gridSize, double minLon, double minLat, double maxLon, double maxLat) {
        if (gridSize <= 0 || maxLon <= minLon || maxLat <= minLat) {
            throw new IllegalArgumentException("Invalid grid parameters provided.");
        }
        this.gridSize = gridSize;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;

        // 计算网格维度
        double lonStep = gridSizeInLongitude(gridSize);
        double latStep = gridSizeInLatitude(gridSize);
        // Ensure at least 1 row/col even if span is very small
        this.cols = Math.max(1, (int) Math.ceil((maxLon - minLon) / lonStep));
        this.rows = Math.max(1, (int) Math.ceil((maxLat - minLat) / latStep));

        // 初始化网格单元
        this.cells = new GridCell[this.rows][this.cols];
        initCells();
    }

    private void initCells() {
        double lonStep = (maxLon - minLon) / this.cols; // Use calculated steps for consistency
        double latStep = (maxLat - minLat) / this.rows;

        for (int r = 0; r < this.rows; r++) {
            double cellMinLat = minLat + r * latStep;
            // Ensure the last cell aligns with maxLat
            double cellMaxLat = (r == this.rows - 1) ? maxLat : cellMinLat + latStep;

            for (int c = 0; c < this.cols; c++) {
                double cellMinLon = minLon + c * lonStep;
                // Ensure the last cell aligns with maxLon
                double cellMaxLon = (c == this.cols - 1) ? maxLon : cellMinLon + lonStep;

                cells[r][c] = new GridCell(r, c, cellMinLon, cellMinLat, cellMaxLon, cellMaxLat);
            }
        }
    }

    public GridCell getCellByPosition(double lon, double lat) {
        // Use slightly adjusted check to handle points exactly on max boundary falling into the last cell
        if (lon < minLon || lon > maxLon || lat < minLat || lat > maxLat || (lon == maxLon && lon != minLon) || (lat == maxLat && lat != minLat)) {
            // Points exactly on the max boundary (and not also on the min boundary) are considered outside
            // or handled by the index calculation logic correctly assigning them to the last cell.
            // Let's refine index calculation instead of strict boundary check here for points on edge.
            if (lon < minLon || lon > maxLon || lat < minLat || lat > maxLat) return null; // Definitively outside
        }


        // 创建缓存键 (保持不变)
        String cacheKey = String.format("%.5f:%.5f", lon, lat);

        return cellLookupCache.computeIfAbsent(cacheKey, k -> {
            double lonStep = (maxLon - minLon) / this.cols;
            double latStep = (maxLat - minLat) / this.rows;

            // Calculate column index, handle edge case lon == maxLon
            int col = (lon == maxLon) ? this.cols - 1 : (int) ((lon - minLon) / lonStep);
            // Calculate row index, handle edge case lat == maxLat
            int row = (lat == maxLat) ? this.rows - 1 : (int) ((lat - minLat) / latStep);

            // Clamp indices to valid range (should ideally not be needed with correct calculation, but safe)
            col = Math.max(0, Math.min(col, this.cols - 1));
            row = Math.max(0, Math.min(row, this.rows - 1));

            return cells[row][col];
        });
    }

    /**
     * 根据行和列索引获取网格单元。 (添加的方法)
     * @param row 行索引 (0-based)
     * @param col 列索引 (0-based)
     * @return 对应的 GridCell 对象，如果索引无效则返回 null。
     */
    public GridCell getCell(int row, int col) {
        if (row >= 0 && row < this.rows && col >= 0 && col < this.cols) {
            return this.cells[row][col];
        }
        logger.warn("Invalid cell index request: row={}, col={}. Grid dimensions are {}x{}", row, col, this.rows, this.cols);
        return null;
    }


    public List<GridCell> getAllCells() {
        List<GridCell> list = new ArrayList<>(this.rows * this.cols);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                if (cells[r][c] != null) { // Add null check just in case
                    list.add(cells[r][c]);
                }
            }
        }
        return list;
    }

    // 经纬度距离换算保持不变
    private double gridSizeInLongitude(double km) { return km / 85.3; } // Approximate for Beijing
    private double gridSizeInLatitude(double km) { return km / 111.0; } // Approximate
}