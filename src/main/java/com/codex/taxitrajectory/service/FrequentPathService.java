package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.core.Grid;
import com.codex.taxitrajectory.model.core.GridCell;
import com.codex.taxitrajectory.model.core.Path; // 你的 Path 类
import com.codex.taxitrajectory.model.core.PathFrequency; // 你的 PathFrequency 类
import com.codex.taxitrajectory.model.query.FrequentPathQuery; // 我们定义的 Query 类
import com.codex.taxitrajectory.model.result.FrequentPathResult; // 你的 Result 类
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils; // 假设 GeoUtils 可用

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for analyzing frequent taxi paths (F7 and F8) using direct sequence counting.
 */
@Service
public class FrequentPathService {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathService.class);

    private final TaxiRepository taxiRepository;

    // 注入配置
    @Value("${map.bounds.minLongitude:116.0}") private double mapMinLon;
    @Value("${map.bounds.minLatitude:39.7}") private double mapMinLat;
    @Value("${map.bounds.maxLongitude:116.8}") private double mapMaxLon;
    @Value("${map.bounds.maxLatitude:40.2}") private double mapMaxLat;
    @Value("${trajectory.analysis.segmentation.max_time_gap_minutes:30}") private long maxTimeGapMinutes;


    @Value("${map.gridSizeMeters:500}") // 从配置注入网格大小（500m)，提供默认值
    private double configuredGridSizeMeters;

    @Value("${trajectory.analysis.minPathLengthCells:3}") // 从配置注入最小格子数，提供默认值
    private int configuredMinPathLengthCells;

    @Autowired
    public FrequentPathService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    /**
     * 主入口方法：分析 Top-K 频繁路径 (F7 或 F8).
     *
     * @param query 查询参数 (FrequentPathQuery).
     * @return 分析结果 (FrequentPathResult).
     */
    public FrequentPathResult analyzeFrequentPaths(FrequentPathQuery query) {
        // 1. 参数校验
        if (!query.isValid()) {
            logger.warn("Invalid FrequentPathQuery received (k, minDistance, regions, time): {}", query);
            return new FrequentPathResult(Collections.emptyList());
        }

        long analysisStartTime = System.nanoTime();
        logger.info("Starting frequent path analysis with query: {}", query);

        // 2. 创建 Grid 对象
        Grid grid = new Grid(configuredGridSizeMeters / 1000.0, mapMinLon, mapMinLat, mapMaxLon, mapMaxLat);
        logger.info("Grid created: {} rows, {} cols, cell size approx {} meters (from config)",
                grid.getRows(), grid.getCols(), configuredGridSizeMeters);

        // 3. 获取 Taxi IDs
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (allTaxiIds.isEmpty()) {
            logger.warn("No taxi IDs found.");
            return new FrequentPathResult(Collections.emptyList());
        }
        logger.info("Processing {} taxi trajectories.", allTaxiIds.size());

        // 4. 初始化计数的 Map
        ConcurrentHashMap<Path, AtomicInteger> pathCounts = new ConcurrentHashMap<>();

        // 5. 并行处理
        allTaxiIds.parallelStream().forEach(taxiId -> {
            try {
                processSingleTaxi(taxiId, query, grid, pathCounts);
            } catch (Exception e) {
                logger.error("Error processing taxi {}: {}", taxiId, e.getMessage(), e);
            }
        });

        logger.info("Finished processing all taxis. Found {} unique path sequences.", pathCounts.size());

        // 6. 排序并映射到 PathFrequency
        List<PathFrequency> topPaths = pathCounts.entrySet().stream()
                .sorted(Map.Entry.<Path, AtomicInteger>comparingByValue(Comparator.comparingInt(AtomicInteger::get)).reversed())
                .limit(query.getK())
                .map(entry -> new PathFrequency(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());

        long analysisEndTime = System.nanoTime();
        long durationMillis = (analysisEndTime - analysisStartTime) / 1_000_000;
        logger.info("Frequent path analysis (Method B) completed in {} ms. Found top {} paths.", durationMillis, topPaths.size());

        // 7. 返回结果
        return new FrequentPathResult(topPaths);
    }

    // --- 以下是辅助方法 ---

    private void processSingleTaxi(String taxiId, FrequentPathQuery query, Grid grid, ConcurrentHashMap<Path, AtomicInteger> pathCounts) {
        List<TaxiRecord> trajectory = taxiRepository.getRecordsByTaxiIdAsList(taxiId);
        if (trajectory == null || trajectory.size() < 2) return;

        List<List<TaxiRecord>> segments = segmentTrajectory(trajectory, maxTimeGapMinutes);

        for (List<TaxiRecord> segment : segments) {
            if (segment.size() >= 2) {
                processSegment(segment, query, grid, pathCounts);
            }
        }
    }

    private List<List<TaxiRecord>> segmentTrajectory(List<TaxiRecord> trajectory, long maxTimeGapMinutes) {
        List<List<TaxiRecord>> segments = new ArrayList<>();
        if (trajectory == null || trajectory.isEmpty()) return segments;

        List<TaxiRecord> currentSegment = new ArrayList<>();
        currentSegment.add(trajectory.get(0));

        for (int i = 1; i < trajectory.size(); i++) {
            TaxiRecord prevRecord = trajectory.get(i - 1);
            TaxiRecord currentRecord = trajectory.get(i);

            if (prevRecord.getTimestamp() == null || currentRecord.getTimestamp() == null) {
                logger.warn("Skipping record due to null timestamp for taxi {} at index {}", prevRecord.getTaxiId(), i);
                continue;
            }

            Duration timeDiff = Duration.between(prevRecord.getTimestamp(), currentRecord.getTimestamp());

            if (timeDiff.toMinutes() > maxTimeGapMinutes) {
                if (currentSegment.size() >= 2) {
                    segments.add(new ArrayList<>(currentSegment));
                }
                currentSegment.clear();
            }
            currentSegment.add(currentRecord);
        }

        if (currentSegment.size() >= 2) {
            segments.add(currentSegment);
        }
        return segments;
    }

    private void processSegment(List<TaxiRecord> segment, FrequentPathQuery query, Grid grid, ConcurrentHashMap<Path, AtomicInteger> pathCounts) {
        if (segment == null || segment.size() < 2) return;

        List<GridCell> pathCells = new ArrayList<>();
        GridCell lastAddedCell = null;

        // 1. 网格化与去重
        for (TaxiRecord record : segment) {
            GridCell currentCell = grid.getCellByPosition(record.getLongitude(), record.getLatitude());
            if (currentCell != null && (!currentCell.equals(lastAddedCell))) {
                pathCells.add(currentCell);
                lastAddedCell = currentCell;
            }
        }

        // 2. 过滤: 长度 (格子数)
        if (pathCells.size() < configuredMinPathLengthCells) return;

        // 3. 过滤: 距离 (米)
        double pathDistanceMeters = calculatePathDistance(segment);
        if (pathDistanceMeters < query.getMinPathDistanceMeters()) return;

        // 4. 过滤: F8 区域
        if (query.isRegionQuery()) {
            if (!isPathInRegions(pathCells, query.getRegionA(), query.getRegionB())) return;
        }

        // 5. 创建 Path 对象
        Path pathKey = createPathFromGridCells(pathCells);

        // 6. 更新计数
        if (pathKey != null) {
            pathCounts.computeIfAbsent(pathKey, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    private Path createPathFromGridCells(List<GridCell> pathCells) {
        if (pathCells == null || pathCells.isEmpty()) {
            return null;
        }
        List<String> cellIdSequence = pathCells.stream()
                .map(cell -> cell.getRow() + "," + cell.getCol())
                .collect(Collectors.toList());
        return new Path(cellIdSequence);
    }

    private double calculatePathDistance(List<TaxiRecord> pathRecords) {
        double totalDistance = 0.0;
        if (pathRecords == null || pathRecords.size() < 2) return totalDistance;
        for (int i = 1; i < pathRecords.size(); i++) {
            TaxiRecord p1 = pathRecords.get(i - 1);
            TaxiRecord p2 = pathRecords.get(i);
            if (p1 != null && p2 != null) {
                totalDistance += GeoUtils.calculateDistance(p1,p2);
            }
        }
        return totalDistance;
    }

    private boolean isPathInRegions(List<GridCell> pathCells, Region regionA, Region regionB) {
        if (pathCells == null || pathCells.size() < 1 || regionA == null || regionB == null) { // Need at least 1 cell
            return false;
        }
        GridCell startCell = pathCells.get(0);
        GridCell endCell = pathCells.get(pathCells.size() - 1);

        boolean startInA = GeoUtils.isPointInRegion(startCell.getCenterLat(), startCell.getCenterLon(), regionA);
        boolean endInB = GeoUtils.isPointInRegion(endCell.getCenterLat(), endCell.getCenterLon(), regionB);

        return startInA && endInB;
    }
}