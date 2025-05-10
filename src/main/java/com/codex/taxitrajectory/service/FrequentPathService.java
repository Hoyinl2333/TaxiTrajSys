package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Grid;
import com.codex.taxitrajectory.model.core.GridCell;
import com.codex.taxitrajectory.model.core.Path;
import com.codex.taxitrajectory.model.core.PathFrequency; // 确保这是修改后的版本
import com.codex.taxitrajectory.model.core.PointCoordinate; // 新引入的坐标点类
import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord; // 确保经纬度是 Double 类型
import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils; // 假设 GeoUtils 可用且包含所需方法

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 频繁路径分析服务 (F7 和 F8 功能)。
 * <p>
 * 该服务通过将出租车轨迹映射到网格序列，并统计这些序列出现的频率，
 * 来识别城市范围内的热门路径或特定区域间的热门路径。
 * 最终结果中的路径将以网格中心点坐标序列的形式提供，方便前端可视化。
 * </p>
 */
@Service
public class FrequentPathService {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathService.class);

    private final TaxiRepository taxiRepository;
    private final GeoUtils geoUtils; // 显式注入GeoUtils

    // --- 地图与网格配置 ---
    @Value("${map.bounds.minLongitude:116.0}")
    private double mapMinLongitude;
    @Value("${map.bounds.minLatitude:39.7}")
    private double mapMinLatitude;
    @Value("${map.bounds.maxLongitude:116.8}")
    private double mapMaxLongitude;
    @Value("${map.bounds.maxLatitude:40.2}")
    private double mapMaxLatitude;

    /**
     * 从配置注入的期望网格大小（单位：km）。
     * 例如：0.5 表示期望每个网格单元的边长大约0.5千米。
     */
    @Value("${map.gridSizeKM:0.5}")
    private double configuredGridSizeKM;

    // --- 轨迹分析配置 ---
    /**
     * 从配置注入的行程分割最大时间间隔（单位：分钟）。
     * 如果轨迹中连续两个点的时差超过此值，则认为是一个新的行程段。
     */
    @Value("${trajectory.analysis.segmentation.max_time_gap_minutes:30}")
    private long maxTimeGapMinutesBetweenRecords;

    /**
     * 从配置注入的有效路径所需包含的最少网格单元数（去重后）。
     * 用于过滤掉过短或意义不大的网格序列。
     */
    @Value("${trajectory.analysis.minPathLengthCells:3}")
    private int minPathLengthInCells;

    @Autowired
    public FrequentPathService(TaxiRepository taxiRepository, GeoUtils geoUtils) {
        this.taxiRepository = taxiRepository;
        this.geoUtils = geoUtils;
    }

    /**
     * 分析Top-K频繁路径的主入口方法。
     * 根据查询参数决定执行全市范围分析 (F7) 还是区域间分析 (F8)。
     *
     * @param query 包含分析参数的查询对象 ({@link FrequentPathQuery})。
     * @return 包含Top-K频繁路径及其频率的分析结果 ({@link FrequentPathResult})。
     */
    public FrequentPathResult analyzeFrequentPaths(FrequentPathQuery query) {
        if (!query.isValid()) {
            logger.warn("接收到无效的频繁路径查询参数 (FrequentPathQuery): {}", query);
            return new FrequentPathResult(Collections.emptyList());
        }

        long analysisStartTimeNanos = System.nanoTime();
        logger.info("=============================");
        logger.info("开始频繁路径分析，查询参数: {}", query);

        Grid grid = new Grid(configuredGridSizeKM, mapMinLongitude, mapMinLatitude, mapMaxLongitude, mapMaxLatitude);
        logger.info("网格已创建：{} 行 x {} 列，期望单元格大小约 {} 千米 (来自配置)",
                grid.getRows(), grid.getCols(), configuredGridSizeKM); // 使用 getRowCount() 和 getColumnCount()

        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (allTaxiIds.isEmpty()) {
            logger.warn("未找到任何出租车ID，无法进行分析。");
            return new FrequentPathResult(Collections.emptyList());
        }
        logger.info("准备处理 {} 辆出租车的轨迹数据。", allTaxiIds.size());

        ConcurrentHashMap<Path, AtomicInteger> pathCounts = new ConcurrentHashMap<>();

        allTaxiIds.parallelStream().forEach(taxiId -> {
            try {
                // 方法名调整回 processSingleTaxi
                processSingleTaxi(taxiId, query, grid, pathCounts);
            } catch (Exception e) {
                logger.error("处理出租车 {} 的轨迹时发生错误: {}", taxiId, e.getMessage(), e);
            }
        });

        logger.info("所有出租车轨迹处理完毕。共发现 {} 条不同的路径序列。", pathCounts.size());

        List<PathFrequency> topPathsWithCoordinates = pathCounts.entrySet().stream()
                .sorted(Map.Entry.<Path, AtomicInteger>comparingByValue(Comparator.comparingInt(AtomicInteger::get)).reversed())
                .limit(query.getK())
                .map(entry -> {
                    Path pathObject = entry.getKey();
                    int frequency = entry.getValue().get();
                    List<PointCoordinate> coordinates = convertPathToCoordinates(pathObject, grid);
                    if (coordinates.isEmpty() && !pathObject.getCellIdSequence().isEmpty()) {
                        logger.warn("路径 (ID序列: {}) 无法转换成有效的坐标点序列，但其频率为 {}。可能某些网格单元无效或路径本身为空。",
                                pathObject.getCellIdSequence(), frequency);
                    }
                    return new PathFrequency(coordinates, frequency);
                })
                .collect(Collectors.toList());

        long analysisEndTimeNanos = System.nanoTime();
        long durationMillis = (analysisEndTimeNanos - analysisStartTimeNanos) / 1_000_000;
        logger.info("频繁路径分析完成，耗时 {} 毫秒。找到 Top {} 条路径 (已转换为坐标序列)。", durationMillis, topPathsWithCoordinates.size());

        return new FrequentPathResult(topPathsWithCoordinates);
    }

    /**
     * 处理单辆出租车的完整轨迹数据。
     * (方法名从 processSingleTaxiTrajectory 调整回 processSingleTaxi)
     *
     * @param taxiId     出租车ID。
     * @param query      当前的查询参数。
     * @param grid       地图网格对象。
     * @param pathCounts 用于累加路径频率的并发Map。
     */
    private void processSingleTaxi(String taxiId, FrequentPathQuery query, Grid grid, ConcurrentHashMap<Path, AtomicInteger> pathCounts) {
        List<TaxiRecord> trajectory = taxiRepository.getRecordsByTaxiIdAsList(taxiId);

        if (trajectory == null || trajectory.size() < 2) {
            return;
        }
        // 方法名从 segmentTrajectoryIntoTrips 调整回 segmentTrajectory
        List<List<TaxiRecord>> segments = segmentTrajectory(trajectory, maxTimeGapMinutesBetweenRecords);

        int segmentCount = 0;
        for (List<TaxiRecord> segmentData : segments) { // 变量名 segment 改为 segmentData 避免与方法名冲突
            segmentCount++;
            if (segmentData.size() >= 2) {
                // 方法名从 processTripSegment 调整回 processSegment
                processSegment(segmentData, query, grid, pathCounts, taxiId, segmentCount);
            }
        }
    }

    /**
     * 将连续的轨迹点列表根据时间间隔分割成多个行程段。
     * (方法名从 segmentTrajectoryIntoTrips 调整回 segmentTrajectory)
     *
     * @param trajectory          一条（通常是完整的）出租车轨迹，按时间顺序排列。
     * @param maxTimeGapInMinutes 允许的最大时间间隔（分钟），超过此间隔则认为是一个新的行程。
     * @return 由多个行程段（每个行程段是一个 {@code List<TaxiRecord>}）组成的列表。
     */
    protected List<List<TaxiRecord>> segmentTrajectory(List<TaxiRecord> trajectory, long maxTimeGapInMinutes) {
        List<List<TaxiRecord>> tripSegments = new ArrayList<>();
        if (trajectory == null || trajectory.isEmpty()) {
            return tripSegments;
        }

        List<TaxiRecord> currentSegment = new ArrayList<>();
        currentSegment.add(trajectory.get(0));

        for (int i = 1; i < trajectory.size(); i++) {
            TaxiRecord previousRecord = trajectory.get(i - 1);
            TaxiRecord currentRecord = trajectory.get(i);

            if (previousRecord.getTimestamp() == null || currentRecord.getTimestamp() == null) {
                logger.warn("出租车 {} 的轨迹点 (索引 {}) 存在空时间戳，跳过该点。",
                        (previousRecord.getTaxiId() != null ? previousRecord.getTaxiId() : "未知"), i);
                if (currentSegment.size() >= 2) {
                    tripSegments.add(new ArrayList<>(currentSegment));
                }
                currentSegment.clear();
                if(currentRecord.getTimestamp() != null) {
                    currentSegment.add(currentRecord);
                }
                continue;
            }

            Duration timeDifference = Duration.between(previousRecord.getTimestamp(), currentRecord.getTimestamp());

            if (timeDifference.toMinutes() > maxTimeGapInMinutes) {
                if (currentSegment.size() >= 2) {
                    tripSegments.add(new ArrayList<>(currentSegment));
                }
                currentSegment.clear();
            }
            currentSegment.add(currentRecord);
        }

        if (currentSegment.size() >= 2) {
            tripSegments.add(currentSegment);
        }
        return tripSegments;
    }

    /**
     * 处理单个行程段，将其转换为网格序列并进行过滤和计数。
     * (方法名从 processTripSegment 调整回 processSegment)
     *
     * @param segmentData  一个行程段的轨迹点列表 (变量名 segment 改为 segmentData)。
     * @param query        当前的查询参数。
     * @param grid         地图网格对象。
     * @param pathCounts   用于累加路径频率的并发Map。
     * @param taxiId       当前处理的出租车ID (用于日志)。
     * @param segmentIndex 当前行程段的索引 (用于日志)。
     */
    private void processSegment(List<TaxiRecord> segmentData, // 变量名 segment 改为 segmentData
                                FrequentPathQuery query,
                                Grid grid,
                                ConcurrentHashMap<Path, AtomicInteger> pathCounts,
                                String taxiId,
                                int segmentIndex) {
        if (segmentData == null || segmentData.size() < 2) {
            return;
        }

        List<GridCell> pathCells = new ArrayList<>();
        GridCell lastAddedCell = null;
        for (TaxiRecord record : segmentData) {
            // 确保 TaxiRecord 中的经纬度是 Double 类型，以便进行 null 检查
            if (record.getLongitude() == null || record.getLatitude() == null) {
                logger.warn("出租车 {} 的行程段 {} 中存在GPS坐标为空的记录，跳过该记录。记录详情: {}", taxiId, segmentIndex, record);
                continue;
            }
            GridCell currentCell = grid.getCellByPosition(record.getLongitude(), record.getLatitude());
            if (currentCell != null) {
                if (!currentCell.equals(lastAddedCell)) {
                    pathCells.add(currentCell);
                    lastAddedCell = currentCell;
                }
            }
        }

        if (pathCells.size() < minPathLengthInCells) {
            return;
        }
        // 方法名从 calculatePathDistanceInMeters 调整回 calculatePathDistance
        double actualPathDistanceKM = calculatePathDistance(segmentData);
        if (actualPathDistanceKM < query.getMinPathDistanceKM()) {
            return;
        }

        if (query.isRegionQuery()) {
            if (query.getRegionA() == null || query.getRegionB() == null) {
                logger.warn("F8区域查询中，起始区域A或目标区域B未定义，无法过滤。出租车 {}，行程段 {}", taxiId, segmentIndex);
                return;
            }
            // 方法名从 doesPathStartInRegionAAndEndInRegionB 调整回 isPathInRegions
            if (!isPathInRegions(pathCells, query.getRegionA(), query.getRegionB())) {
                return;
            }
        }
        // 方法名从 createPathFromGridCellSequence 调整回 createPathFromGridCells
        Path pathKey = createPathFromGridCells(pathCells);
        if (pathKey == null || pathKey.getCellIdSequence().isEmpty()) {
            logger.warn("从网格单元列表未能成功创建Path对象。出租车 {}，行程段 {}", taxiId, segmentIndex);
            return;
        }

        pathCounts.computeIfAbsent(pathKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 从网格单元列表 ({@code List<GridCell>}) 创建一个 {@link Path} 对象。
     * (方法名从 createPathFromGridCellSequence 调整回 createPathFromGridCells)
     *
     * @param pathCells 经过网格化和去重的网格单元列表。
     * @return 代表该路径的 Path 对象；如果输入列表为空或无效，则返回 null。
     */
    private Path createPathFromGridCells(List<GridCell> pathCells) {
        if (pathCells == null || pathCells.isEmpty()) {
            return null;
        }
        List<String> cellIdSequence = pathCells.stream()
                .map(cell -> cell.getRow() + "," + cell.getCol())
                .collect(Collectors.toList());
        return new Path(cellIdSequence);
    }

    /**
     * 计算一个轨迹段的实际累计地理距离。
     * (方法名从 calculatePathDistanceInMeters 调整回 calculatePathDistance)
     *
     * @param pathRecords 轨迹段中的 {@link TaxiRecord} 列表。
     * @return 累计距离（单位：千米）。
     */
    private double calculatePathDistance(List<TaxiRecord> pathRecords) {
        double totalDistance = 0.0;
        if (pathRecords == null || pathRecords.size() < 2) {
            return totalDistance;
        }
        for (int i = 1; i < pathRecords.size(); i++) {
            TaxiRecord p1 = pathRecords.get(i - 1);
            TaxiRecord p2 = pathRecords.get(i);
            if (p1 != null && p2 != null &&
                    p1.getLongitude() != null && p1.getLatitude() != null && // 确保是 Double 类型进行 null 检查
                    p2.getLongitude() != null && p2.getLatitude() != null) {
                totalDistance += GeoUtils.calculateDistance(
                        p1.getLatitude(), p1.getLongitude(),
                        p2.getLatitude(), p2.getLongitude()
                );
            }
        }
        return totalDistance;
    }

    /**
     * 检查给定的网格化路径是否从区域A开始并在区域B结束。
     * (方法名从 doesPathStartInRegionAAndEndInRegionB 调整回 isPathInRegions)
     *
     * @param pathCells 网格化后的路径单元格列表。
     * @param regionA   起始区域。
     * @param regionB   目标区域。
     * @return 如果路径满足起止区域条件，则返回 true；否则返回 false。
     */
    private boolean isPathInRegions(List<GridCell> pathCells, Region regionA, Region regionB) {
        if (pathCells == null || pathCells.isEmpty() || regionA == null || regionB == null) {
            return false;
        }
        GridCell startCell = pathCells.get(0);
        GridCell endCell = pathCells.get(pathCells.size() - 1);

        boolean startsInA = geoUtils.isPointInRegion(
                startCell.getCenterLat(), startCell.getCenterLon(), regionA
        );
        if (!startsInA) return false;

        boolean endsInB = geoUtils.isPointInRegion(
                endCell.getCenterLat(), endCell.getCenterLon(), regionB
        );
        return endsInB;
    }

    /**
     * 将 {@link Path} 对象（内部为网格ID序列）转换为网格中心点坐标 ({@link PointCoordinate}) 的列表。
     *
     * @param pathObject 包含网格ID序列的 Path 对象。
     * @param grid       当前的 Grid 对象，用于查找网格单元。
     * @return 转换后的坐标点列表；如果无法转换，可能返回空列表。
     */
    private List<PointCoordinate> convertPathToCoordinates(Path pathObject, Grid grid) {
        if (pathObject == null || pathObject.getCellIdSequence() == null || pathObject.getCellIdSequence().isEmpty()) {
            return Collections.emptyList();
        }

        List<PointCoordinate> coordinates = new ArrayList<>();
        for (String cellId : pathObject.getCellIdSequence()) {
            String[] parts = cellId.split(",");
            if (parts.length == 2) {
                try {
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);

                    GridCell cell = grid.getCell(row, col);

                    if (cell != null) {
                        coordinates.add(new PointCoordinate(cell.getCenterLon(), cell.getCenterLat()));
                    } else {
                        logger.warn("路径转换坐标：无法为网格ID (row={}, col={}) 找到对应的 GridCell。原始CellID字符串: '{}'。路径序列: {}",
                                row, col, cellId, pathObject.getCellIdSequence());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("路径转换坐标：无效的网格ID格式 '{}' (无法解析行列号)。路径序列: {}. 错误: {}",
                            cellId, pathObject.getCellIdSequence(), e.getMessage());
                }
            } else {
                logger.warn("路径转换坐标：无效的网格ID格式 '{}' (非 'row,col' 格式)。路径序列: {}",
                        cellId, pathObject.getCellIdSequence());
            }
        }
        return coordinates;
    }
}
