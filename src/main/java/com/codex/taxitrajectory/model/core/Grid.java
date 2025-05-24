package com.codex.taxitrajectory.model.core;

import lombok.Data; // 自动生成 getter, setter, toString, equals, hashCode, 以及 final 字段的构造函数

import java.util.ArrayList;
import java.util.List;

/**
 * 网格管理类。
 * <p>
 * 负责根据给定的地理范围和期望的网格单元大小（公里），将地图区域动态划分为二维网格单元。
 * 提供通过地理坐标或行列索引查询特定网格单元的功能。
 * 此类在构造后，其网格结构和边界是不可变的。
 * </p>
 */
@Data
public class Grid {

    /**
     * 存储所有网格单元的二维数组 (cells[row][col])。
     */
    private final GridCell[][] cells;

    /**
     * 用户期望的网格单元边长，单位：千米 (km)。
     */
    private final double gridSize;

    /**
     * 网格覆盖范围的最小经度。
     */
    private final double minLon;
    /**
     * 网格覆盖范围的最小纬度。
     */
    private final double minLat;
    /**
     * 网格覆盖范围的最大经度。
     */
    private final double maxLon;
    /**
     * 网格覆盖范围的最大纬度。
     */
    private final double maxLat;

    /**
     * 网格的总行数。
     */
    private final int rows;
    /**
     * 网格的总列数。
     */
    private final int cols;

    /**
     * 每个网格单元在经度方向上的实际跨度（单位：度）。
     */
    private final double actualLonStepPerCell;
    /**
     * 每个网格单元在纬度方向上的实际跨度（单位：度）。
     */
    private final double actualLatStepPerCell;

    /**
     * 每公里对应的纬度变化量（度，近似恒定值）。
     * 大约 1 / 111.0 度/公里。
     */
    private static final double DEGREES_PER_KM_LAT = 1.0 / 111.0;


    /**
     * 构造一个新的 Grid 对象。
     *
     * @param gridSize 网格大小（公里）。必须大于0。
     * @param minLon     网格范围的最小经度。
     * @param minLat     网格范围的最小纬度。
     * @param maxLon     网格范围的最大经度。必须大于 minLon。
     * @param maxLat     网格范围的最大纬度。必须大于 minLat。
     * @throws IllegalArgumentException 如果参数无效。
     */
    public Grid(double gridSize, double minLon, double minLat, double maxLon, double maxLat) {
        if (gridSize <= 0) {
            throw new IllegalArgumentException("网格大小 (gridSize) 必须大于 0。");
        }
        if (maxLon <= minLon) {
            throw new IllegalArgumentException("最大经度 (maxLon) 必须大于最小经度 (minLon)。");
        }
        if (maxLat <= minLat) {
            throw new IllegalArgumentException("最大纬度 (maxLat) 必须大于最小纬度 (minLat)。");
        }

        this.gridSize = gridSize;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;

        // 基于期望的 gridSize 计算在当前区域平均情况下的经纬度步长
        double expectedLonStepPerCell = gridSizeToDegreesLon(gridSize, (minLat + maxLat) / 2.0);
        double expectedLatStepPerCell = gridSizeToDegreesLat(gridSize);

        // 根据总范围和期望步长计算行列数，确保至少有1行1列
        this.cols = Math.max(1, (int) Math.ceil((this.maxLon - this.minLon) / expectedLonStepPerCell));
        this.rows = Math.max(1, (int) Math.ceil((this.maxLat - this.minLat) / expectedLatStepPerCell));

        // 计算每个网格单元的实际经纬度步长
        // 由于 cols 和 rows 保证至少为1，所以此处除数不为0
        this.actualLonStepPerCell = (this.maxLon - this.minLon) / this.cols;
        this.actualLatStepPerCell = (this.maxLat - this.minLat) / this.rows;

        this.cells = new GridCell[this.rows][this.cols];
        initializeCells();
    }

    /**
     * 初始化所有网格单元 (GridCell)。
     * 此方法在构造函数中调用，为每个单元格计算并设置其精确的地理边界。
     */
    private void initializeCells() {
        for (int r = 0; r < this.rows; r++) {
            double cellMinLat = this.minLat + r * actualLatStepPerCell;
            // 为避免浮点数累积误差，最后一行的maxLat直接使用网格的maxLat
            double cellMaxLat = (r == this.rows - 1) ? this.maxLat : (cellMinLat + actualLatStepPerCell);

            for (int c = 0; c < this.cols; c++) {
                double cellMinLon = this.minLon + c * actualLonStepPerCell;
                // 最后一列的maxLon直接使用网格的maxLon
                double cellMaxLon = (c == this.cols - 1) ? this.maxLon : (cellMinLon + actualLonStepPerCell);

                this.cells[r][c] = new GridCell(r, c, cellMinLon, cellMinLat, cellMaxLon, cellMaxLat);
            }
        }
    }

    /**
     * 根据地理坐标（经度和纬度）获取其所在的网格单元。
     * <p>
     * 坐标点处理规则：
     * - 如果点恰好落在两个单元格共享的垂直边界上，它将被分配给右侧（经度较大）的单元格，除非它位于整个网格的最右边界。
     * - 如果点恰好落在两个单元格共享的水平边界上，它将被分配给上方（纬度较大）的单元格，除非它位于整个网格的最上边界。
     * - 点恰好在 {@code maxLon} 或 {@code maxLat} 上时，会被归入最后一行/列的单元格。
     * </p>
     *
     * @param lon 经度。
     * @param lat 纬度。
     * @return 对应的 {@link GridCell} 对象；如果坐标超出网格定义的整体范围，则返回 null。
     */
    public GridCell getCellByPosition(double lon, double lat) {
        // 1. 快速边界检查
        if (lon < this.minLon || lon > this.maxLon || lat < this.minLat || lat > this.maxLat) {
            if (lon > this.maxLon || lat > this.maxLat || lon < this.minLon || lat < this.minLat) { // 严格超出
                return null;
            }
        }

        // 2. 计算行列索引
        int colIndex;
        if (lon == this.maxLon) { // 点恰好在最大经度边界上
            colIndex = this.cols - 1;    // 归入最后一列
        } else {
            // actualLonStepPerCell 不为0 (因为 cols >= 1)
            colIndex = (int) ((lon - this.minLon) / this.actualLonStepPerCell);
        }

        int rowIndex;
        if (lat == this.maxLat) { // 点恰好在最大纬度边界上
            rowIndex = this.rows - 1;    // 归入最后一行
        } else {
            // actualLatStepPerCell 不为0 (因为 rows >= 1)
            rowIndex = (int) ((lat - this.minLat) / this.actualLatStepPerCell);
        }

        // 3. 确保索引在有效范围内
        colIndex = Math.max(0, Math.min(colIndex, this.cols - 1));
        rowIndex = Math.max(0, Math.min(rowIndex, this.rows - 1));

        return this.cells[rowIndex][colIndex];
    }

    /**
     * 根据行和列索引获取指定的网格单元。
     *
     * @param row 行索引 (0-based)。
     * @param col 列索引 (0-based)。
     * @return 对应的 {@link GridCell} 对象；如果索引无效（越界），则返回 null。
     */
    public GridCell getCell(int row, int col) {
        if (row >= 0 && row < this.rows && col >= 0 && col < this.cols) {
            return this.cells[row][col];
        }
        return null;
    }

    /**
     * 获取网格中所有单元格的列表。
     * 主要用于需要遍历所有单元格的场景。
     *
     * @return 包含所有 {@link GridCell} 对象的列表。列表中的单元格顺序为先行后列。
     */
    public List<GridCell> getAllCells() {
        List<GridCell> allCellsList = new ArrayList<>(this.rows * this.cols);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                allCellsList.add(this.cells[r][c]);
            }
        }
        return allCellsList;
    }

    /**
     * 将公里数转换为在指定纬度下对应的经度跨度（单位：度）。
     *
     * @param km 距离，单位公里。
     * @param latitude 当前位置的纬度。
     * @return 对应的经度跨度（度）。
     */
    private double gridSizeToDegreesLon(double km, double latitude) {
        if (km <= 0) return 0.0;
        // 1度的经度距离（公里） = 111.320 * cos(latitude_in_radians)
        double kmPerDegreeLonAtLat = 111.320 * Math.cos(Math.toRadians(latitude));
        if (kmPerDegreeLonAtLat <= 1e-6) {
            // 在极点附近，经度的意义减弱，返回一个极大的度数，使列数趋于1。
            return 360.0;
        }
        return km / kmPerDegreeLonAtLat;
    }

    /**
     * 将公里数转换为对应的纬度跨度（单位：度）。
     *
     * @param km 公里数。
     * @return 对应的纬度跨度（度）。
     */
    private double gridSizeToDegreesLat(double km) {
        if (km <= 0) return 0.0;
        return km * DEGREES_PER_KM_LAT;
    }

    // @Data 注解会为所有 final 字段自动生成 getter 方法，例如 getRows() 和 getCols()。
    // 无需显式定义。
}