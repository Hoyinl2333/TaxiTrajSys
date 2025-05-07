package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.Getter; // 仅使用 @Getter，因为大部分字段是final的，不需要setter
// import lombok.EqualsAndHashCode; // 如果Grid对象本身需要比较，可以考虑添加
// import lombok.ToString; // 如果需要toString方法

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网格管理类
 * <p>
 * 负责根据给定的地理范围和网格大小，将地图区域划分为二维网格单元。
 * 提供通过地理坐标或行列索引查询特定网格单元的功能。
 * 此类在构造后是线程安全的，其状态（网格结构和边界）不可变。
 * </p>
 */
@Data
public class Grid {
    private static final Logger logger = LoggerFactory.getLogger(Grid.class);

    /**
     * 存储所有网格单元的二维数组。
     * cells[row][col]
     */
    private final GridCell[][] cells;

    /**
     * 网格大小，单位：公里 (km)。
     * 指的是期望的网格单元在地面上的边长。
     */
    private final double gridSize;

    /**
     * 网格覆盖范围的最小经度（左边界）。
     */
    private final double minLon;

    /**
     * 网格覆盖范围的最小纬度（下边界）。
     */
    private final double minLat;

    /**
     * 网格覆盖范围的最大经度（右边界）。
     */
    private final double maxLon;

    /**
     * 网格覆盖范围的最大纬度（上边界）。
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
     * 坐标到网格单元查找的缓存。
     * 使用 ConcurrentHashMap 保证线程安全。
     * 键 (String): "经度:纬度" (格式化，例如 "%.5f:%.5f")
     * 值 (GridCell): 对应的网格单元
     */
    private final ConcurrentHashMap<String, GridCell> cellLookupCache = new ConcurrentHashMap<>();

    // 北京地区近似值：每公里对应的经度变化量（大约值，随纬度变化）
    // 考虑移至 GeoUtils 或作为可配置参数
    private static final double KM_PER_DEGREE_LON_BEIJING = 85.3;
    // 每公里对应的纬度变化量（近似恒定）
    private static final double KM_PER_DEGREE_LAT = 111.0;


    /**
     * 构造一个新的 Grid 对象。
     *
     * @param gridSize 网格大小（公里）。必须大于0。
     * @param minLon     网格范围的最小经度。
     * @param minLat     网格范围的最小纬度。
     * @param maxLon     网格范围的最大经度。必须大于 minLon。
     * @param maxLat     网格范围的最大纬度。必须大于 minLat。
     * @throws IllegalArgumentException 如果参数无效（例如 gridSize <= 0，或地理范围无效）。
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

        // 计算网格的精确步长（每个网格单元在经纬度上的跨度）
        // 注意：这里假设网格是均匀划分整个区域的，而不是每个网格严格为 gridSize * gridSize
        // 如果需要每个网格严格为 gridSize，则计算 rows/cols 的方式会不同，
        // 且总的 maxLon/maxLat 可能会略微超出输入的范围。
        // 目前的实现是基于输入范围和 gridSize 来确定总的行列数。

        // 基于期望的 gridSize 计算在当前区域平均情况下的经纬度步长
        double expectedLonStepPerCell = gridSizeToDegreesLon(gridSize, (minLat + maxLat) / 2.0); // 使用区域中心纬度计算平均经度步长
        double expectedLatStepPerCell = gridSizeToDegreesLat(gridSize);

        // 根据总范围和期望步长计算行列数，确保至少有1行1列
        this.cols = Math.max(1, (int) Math.ceil((this.maxLon - this.minLon) / expectedLonStepPerCell));
        this.rows = Math.max(1, (int) Math.ceil((this.maxLat - this.minLat) / expectedLatStepPerCell));

        logger.info("网格初始化：期望网格大小 {} km，区域 [({}, {}) - ({}, {})]", gridSize, minLon, minLat, maxLon, maxLat);
        logger.info("计算得到的网格维度：{} 行 x {} 列", this.rows, this.cols);

        // 初始化网格单元数组
        this.cells = new GridCell[this.rows][this.cols];
        initializeCells();
    }

    /**
     * 初始化所有网格单元 (GridCell)。
     * 此方法在构造函数中调用。
     */
    private void initializeCells() {
        // 根据最终确定的行列数，重新计算每个网格单元实际的经纬度步长，以确保完全覆盖定义的范围
        double actualLonStepPerCell = (this.maxLon - this.minLon) / this.cols;
        double actualLatStepPerCell = (this.maxLat - this.minLat) / this.rows;

        logger.debug("每个网格单元的实际经度步长: {}, 实际纬度步长: {}", actualLonStepPerCell, actualLatStepPerCell);

        for (int r = 0; r < this.rows; r++) {
            double cellMinLat = this.minLat + r * actualLatStepPerCell;
            // 为了避免浮点数累积误差，最后一行的maxLat直接使用网格的maxLat
            double cellMaxLat = (r == this.rows - 1) ? this.maxLat : cellMinLat + actualLatStepPerCell;

            for (int c = 0; c < this.cols; c++) {
                double cellMinLon = this.minLon + c * actualLonStepPerCell;
                // 最后一列的maxLon直接使用网格的maxLon
                double cellMaxLon = (c == this.cols - 1) ? this.maxLon : cellMinLon + actualLonStepPerCell;

                // 创建 GridCell 对象，传入其准确的边界
                this.cells[r][c] = new GridCell(r, c, cellMinLon, cellMinLat, cellMaxLon, cellMaxLat);
            }
        }
        logger.info("所有网格单元已成功初始化。");
    }

    /**
     * 根据地理坐标（经度和纬度）获取其所在的网格单元。
     * <p>
     * 对于恰好落在网格边界上的点：
     * - 如果点落在两个单元格共享的垂直边界上，它将被分配给右侧（经度较大）的单元格，除非它在整个网格的最右边界。
     * - 如果点落在两个单元格共享的水平边界上，它将被分配给上方（纬度较大）的单元格，除非它在整个网格的最上边界。
     * - 点恰好在 `maxLon` 或 `maxLat` 上时，会被归入最后一行/列的单元格。
     * </p>
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 对应的 GridCell 对象；如果坐标超出网格范围，则返回 null。
     */
    public GridCell getCellByPosition(double lon, double lat) {
        // 1. 快速边界检查 (点是否在整个网格定义的矩形区域内)
        // 注意：允许点恰好等于 maxLon/maxLat，因为它们会被归入最后一个单元格。
        if (lon < this.minLon || lon > this.maxLon || lat < this.minLat || lat > this.maxLat) {
            // 如果点严格大于最大边界，或者严格小于最小边界，则判定为外部
            if (lon > this.maxLon || lat > this.maxLat) {
                logger.trace("坐标 ({}, {}) 超出网格范围 [({}, {}), ({}, {})]", lon, lat, this.minLon, this.minLat, this.maxLon, this.maxLat);
                return null;
            }
            // 对于 lon == maxLon 和 lat == maxLat 的情况，后续逻辑会处理
        }


        // 2. 使用缓存进行查找
        // 格式化缓存键，小数点后5位通常对GPS足够
        String cacheKey = String.format("%.5f:%.5f", lon, lat);
        GridCell cachedCell = cellLookupCache.get(cacheKey);
        if (cachedCell != null) {
            return cachedCell;
        }

        // 3. 如果缓存未命中，则计算行列索引
        // 使用每个网格单元的实际平均步长
        double actualLonStepPerCell = (this.maxLon - this.minLon) / this.cols;
        double actualLatStepPerCell = (this.maxLat - this.minLat) / this.rows;

        int colIndex;
        if (lon == this.maxLon) { // 点恰好在最大经度边界上
            colIndex = this.cols - 1; // 归入最后一列
        } else {
            colIndex = (int) ((lon - this.minLon) / actualLonStepPerCell);
        }

        int rowIndex;
        if (lat == this.maxLat) { // 点恰好在最大纬度边界上
            rowIndex = this.rows - 1; // 归入最后一行
        } else {
            rowIndex = (int) ((lat - this.minLat) / actualLatStepPerCell);
        }

        // 4. 校验计算出的索引是否有效，并从二维数组中获取 GridCell
        // Math.min 和 Math.max 用于防止因浮点精度问题导致的索引轻微越界
        // (理论上，如果 lon/lat 在 [min, max] 区间内，这里的计算结果应该在 [0, count-1] 区间内)
        colIndex = Math.max(0, Math.min(colIndex, this.cols - 1));
        rowIndex = Math.max(0, Math.min(rowIndex, this.rows - 1));

        GridCell resultCell = this.cells[rowIndex][colIndex];

        // 5. 将结果存入缓存
        // computeIfAbsent 的方式更原子性，但既然已经计算出来了，可以直接put
        // 为避免并发问题（虽然此Grid对象构造后是不可变的，但缓存是可变的），
        // 如果多个线程同时计算同一个未缓存的key，用putIfAbsent更好。
        // 但由于GridCell对象本身已创建且是共享的，直接put也可以。
        // 之前您用的 computeIfAbsent 结构更好，这里为了分解步骤写成这样。
        // 若要改回 computeIfAbsent, 则步骤3和4需要在 lambda 表达式内。
        cellLookupCache.put(cacheKey, resultCell);

        return resultCell;
    }

    /**
     * 根据行和列索引获取网格单元。
     *
     * @param row 行索引 (0-based)。
     * @param col 列索引 (0-based)。
     * @return 对应的 GridCell 对象；如果索引无效（越界），则返回 null。
     */
    public GridCell getCell(int row, int col) {
        if (row >= 0 && row < this.rows && col >= 0 && col < this.cols) {
            return this.cells[row][col];
        }
        logger.warn("请求的网格单元索引无效：row={}, col={}。当前网格维度为：{}行 x {}列。", row, col, this.rows, this.cols);
        return null;
    }

    /**
     * 获取网格中所有单元格的列表。
     * 主要用于调试或需要遍历所有单元格的场景。
     *
     * @return 包含所有 GridCell 对象的列表。
     */
    public List<GridCell> getAllCells() {
        List<GridCell> allCellsList = new ArrayList<>(this.rows * this.cols);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                // 在正确初始化的前提下，cells[r][c]不应为null
                allCellsList.add(this.cells[r][c]);
            }
        }
        return allCellsList;
    }

    /**
     * 将公里数转换为对应纬度下的经度跨度（单位：度）。
     * 这是一个近似计算，因为1度经度代表的实际距离随纬度变化。
     *
     * @param km 公里数。
     * @param latitude 纬度，用于计算该纬度下1度经度的公里数。
     * @return 对应的经度跨度（度）。
     */
    private double gridSizeToDegreesLon(double km, double latitude) {
        // 1度的经度距离（公里） = 111.320 * cos(latitude_in_radians)
        // 更通用的计算，而不是依赖特定区域的常数
        if (km <= 0) return 0;
        double kmPerDegreeLon = 111.320 * Math.cos(Math.toRadians(latitude));
        if (kmPerDegreeLon <= 0) { // 避免除以零或负数，例如在极点附近
            // 在极点附近，经度的意义减弱，可以返回一个基于纬度步长的较小值或抛出异常
            // 这里我们使用一个基于纬度步长的替代（简化处理）
            logger.warn("在纬度 {} 计算KM_PER_DEGREE_LON时值过小或为零，可能接近极点。", latitude);
            return gridSizeToDegreesLat(km); // 或者一个更合适的默认小值
        }
        return km / kmPerDegreeLon;
    }

    /**
     * 将公里数转换为对应的纬度跨度（单位：度）。
     * 1度纬度代表的实际距离近似恒定。
     *
     * @param km 公里数。
     * @return 对应的纬度跨度（度）。
     */
    private double gridSizeToDegreesLat(double km) {
        if (km <= 0) return 0;
        return km / KM_PER_DEGREE_LAT; // KM_PER_DEGREE_LAT 约等于 111.0 km/degree
    }


}