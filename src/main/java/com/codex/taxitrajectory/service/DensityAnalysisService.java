package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.*;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.repository.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// TODO：说明时间槽的计算，


@Service
public class DensityAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(DensityAnalysisService.class);

    private final DataLoader dataLoader;
    private final Map<String, DensityAnalysisResult> resultCache = new ConcurrentHashMap<>();

    public DensityAnalysisService(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public DensityAnalysisResult analyzeTrafficDensity(DensityQuery query) {
        String cacheKey = generateCacheKey(query);

        if (resultCache.containsKey(cacheKey)) {
            logger.info("命中缓存，查询参数：{}", cacheKey);
            return resultCache.get(cacheKey);
        }

        logger.info("开始车流密度分析 | 参数：{}", cacheKey);
        long globalStart = System.currentTimeMillis();

        long gridStart = System.currentTimeMillis();
        Grid grid = new Grid(
                query.getGridSize(),
                query.getMinLongitude(),
                query.getMinLatitude(),
                query.getMaxLongitude(),
                query.getMaxLatitude()
        );
        logger.info("网格构建完成，耗时：{} ms", System.currentTimeMillis() - gridStart);

        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();

        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();
        AtomicInteger processedCount = new AtomicInteger(0);
        long filterStart = System.currentTimeMillis();

        allTaxiIds.parallelStream().forEach(taxiId -> {
            long startNano = System.nanoTime();

            NavigableMap<LocalDateTime, TaxiRecord> records = dataLoader.getRecordsByTaxiId(taxiId);
            if (records == null) return;

            NavigableMap<LocalDateTime, TaxiRecord> filtered = records.subMap(startTime, true, endTime, true);
            for (TaxiRecord record : filtered.values()) {
                GridCell cell = grid.getCellByPosition(record.getLongitude(), record.getLatitude());
                if (cell == null) continue;

                LocalDateTime timeSlot = roundToTimeSlot(record.getTimestamp(), timeSlotMinutes);
                cell.addTaxi(record.getTaxiId(), timeSlot);
            }

            long endNano = System.nanoTime();
            long costMillis = (endNano - startNano) / 1_000_000;
            if (costMillis > 1000) {
                logger.warn("出租车 {} 数据处理耗时过长：{} ms", taxiId, costMillis);
            }

            int current = processedCount.incrementAndGet();
            if (current % 1000 == 0) {
                long elapsed = System.currentTimeMillis() - filterStart;
                logger.info("已处理出租车数: {}，当前耗时: {} ms", current, elapsed);
            }
        });

        logger.info("全部出租车数据处理完成，总耗时：{} ms", System.currentTimeMillis() - filterStart);

        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);

        long densityStart = System.currentTimeMillis();
        Map<LocalDateTime, Map<String, Integer>> densityMap = calculateDensityMap(grid, timeSlots);
        long densityCost = System.currentTimeMillis() - densityStart;
        logger.info("密度图计算完成，耗时：{} ms", densityCost);

        int totalGridCount = grid.getAllCells().size();
        int totalTimeSlots = timeSlots.size();
        long resultSize = densityMap.values().stream().mapToInt(Map::size).sum();
        logger.info("结果网格数量：{}，时间段数量：{}，总非空单元格记录数：{}", totalGridCount, totalTimeSlots, resultSize);

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        logger.info("当前JVM内存使用：{} MB", usedMemoryMB);

        DensityAnalysisResult result = new DensityAnalysisResult();
        result.setGrid(grid);
        result.setTimeSlots(timeSlots);
        result.setDensityMap(densityMap);

        if (resultCache.size() > 100) {
            String keyToRemove = resultCache.keySet().iterator().next();
            resultCache.remove(keyToRemove);
        }
        resultCache.put(cacheKey, result);

        long globalCost = System.currentTimeMillis() - globalStart;
        logger.info("车流密度分析完成，总耗时：{} ms", globalCost);
        return result;
    }

    private String generateCacheKey(DensityQuery query) {
        return String.format("%f:%f:%f:%f:%f:%s:%s:%d",
                query.getGridSize(),
                query.getMinLongitude(), query.getMinLatitude(),
                query.getMaxLongitude(), query.getMaxLatitude(),
                query.getStartTime(), query.getEndTime(),
                query.getTimeSlotMinutes());
    }

    private List<LocalDateTime> generateTimeSlots(LocalDateTime start, LocalDateTime end, int intervalMinutes) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            slots.add(current);
            current = current.plusMinutes(intervalMinutes);
        }
        return slots;
    }

    private LocalDateTime roundToTimeSlot(LocalDateTime timestamp, int minutes) {
        int rounded = (timestamp.getMinute() / minutes) * minutes;
        return timestamp.withMinute(rounded).withSecond(0).withNano(0);
    }

    // 性能日志
    private Map<LocalDateTime, Map<String, Integer>> calculateDensityMap(Grid grid, List<LocalDateTime> timeSlots) {
        Map<LocalDateTime, Map<String, Integer>> densityMap = new HashMap<>();
        long start = System.currentTimeMillis();

        for (LocalDateTime timeSlot : timeSlots) {
            long slotStart = System.nanoTime();

            Map<String, Integer> cellDensity = new HashMap<>();
            int nonEmptyCells = 0;
            for (GridCell cell : grid.getAllCells()) {
                int density = cell.getDensity(timeSlot);
                if (density > 0) {
                    String cellKey = cell.getRow() + "," + cell.getCol();
                    cellDensity.put(cellKey, density);
                    nonEmptyCells++;
                }
            }

            densityMap.put(timeSlot, cellDensity);
            long slotCostMs = (System.nanoTime() - slotStart) / 1_000_000;
            if (slotCostMs > 100) {
                logger.warn("时间段 {} 密度计算耗时较长：{} ms，非空单元格数：{}", timeSlot, slotCostMs, nonEmptyCells);
            }
        }

        long total = System.currentTimeMillis() - start;
        logger.info("calculateDensityMap 总耗时：{} ms，时间段数：{}", total, timeSlots.size());
        return densityMap;
    }

    public void clearCache() {
        resultCache.clear();
        logger.info("缓存已清空");
    }
}