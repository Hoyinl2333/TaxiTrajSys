package com.codex.taxitrajectory.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * 网格索引
 * 适用于简单矩形区域查询，内存开销更小，查询效率更高
 */
public class GridIndex {
    private final Map<GridCell, Set<String>> cellToTaxiMap = new HashMap<>();
    private final double cellSize; // 网格大小 (经纬度单位)

    public GridIndex(double cellSize) {
        this.cellSize = cellSize;
    }

    /**
     * 插入GPS点到网格索引
     */
    public void insert(double lon, double lat, String taxiId) {
        GridCell cell = getCellForPoint(lon, lat);
        cellToTaxiMap.computeIfAbsent(cell, k -> new HashSet<>()).add(taxiId);
    }

    /**
     * 区域查询 - 返回区域内所有出租车ID
     */
    public Set<String> query(double minLon, double minLat, double maxLon, double maxLat) {
        Set<String> result = new HashSet<>();

        int minCellX = (int)(minLon / cellSize);
        int minCellY = (int)(minLat / cellSize);
        int maxCellX = (int)(maxLon / cellSize) + 1;
        int maxCellY = (int)(maxLat / cellSize) + 1;

        for (int x = minCellX; x <= maxCellX; x++) {
            for (int y = minCellY; y <= maxCellY; y++) {
                GridCell cell = new GridCell(x, y);
                Set<String> taxis = cellToTaxiMap.get(cell);
                if (taxis != null) {
                    result.addAll(taxis);
                }
            }
        }

        return result;
    }

    /**
     * 根据经纬度计算所属网格
     */
    private GridCell getCellForPoint(double lon, double lat) {
        int cellX = (int)(lon / cellSize);
        int cellY = (int)(lat / cellSize);
        return new GridCell(cellX, cellY);
    }

    /**
     * 网格单元类
     */
    private static class GridCell {
        private final int x;
        private final int y;

        public GridCell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GridCell gridCell = (GridCell) o;
            return x == gridCell.x && y == gridCell.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}
