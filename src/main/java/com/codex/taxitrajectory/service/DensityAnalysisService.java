package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Grid;
import com.codex.taxitrajectory.model.core.GridCell;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.model.result.DensityAnalysisResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.utils.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DensityAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(DensityAnalysisService.class);

    // 从配置文件中读取日志开关，默认启用日志输出
    @Value("${logging.service.enabled:true}")
    private boolean enableLogging;

    private final TaxiRepository taxiRepository;
    //缓存
    private final Map<String, DensityAnalysisResult> resultCache = new ConcurrentHashMap<>();

    //内部维护密度数据
    // Key: 时间槽 (LocalDateTime)
    // Value: Map<String, AtomicInteger> -> Key: "row,col", Value: 密度计数
    // 使用 ConcurrentHashMap 保证并行处理时线程安全
    private final ConcurrentHashMap<LocalDateTime, ConcurrentHashMap<String, AtomicInteger>> densityData = new ConcurrentHashMap<>();

    // **** 注入地图边界配置 ****
    @Value("${map.bounds.minLongitude}")
    private double mapMinLon;
    @Value("${map.bounds.minLatitude}")
    private double mapMinLat;
    @Value("${map.bounds.maxLongitude}")
    private double mapMaxLon;
    @Value("${map.bounds.maxLatitude}")
    private double mapMaxLat;

    @Autowired
    public DensityAnalysisService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    public DensityAnalysisResult analyzeTrafficDensity(DensityQuery query) {
        // 在 Service 入口处调用 query.validate() 进行校验
        try {
            query.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("无效的 DensityQuery 参数: {}", e.getMessage());
            throw e; // 或者 return new DensityAnalysisResult(...error state...);
        }

        String cacheKey = generateCacheKey(query);

        if (resultCache.containsKey(cacheKey)) {
            if (enableLogging) {
                logger.info("命中缓存，查询参数：{}", cacheKey);
            }
            return resultCache.get(cacheKey);
        }

        if (enableLogging) {
            logger.info("开始车流密度分析 | 参数：{}", cacheKey);
        }
        long globalStart = System.currentTimeMillis();

        // 清空上一次的密度数据 (非常重要!)
        densityData.clear();

        long gridStart = System.currentTimeMillis();
        Grid grid = new Grid(
                query.getGridSize(),
                mapMinLon,           // 使用注入的边界
                mapMinLat,           // 使用注入的边界
                mapMaxLon,           // 使用注入的边界
                mapMaxLat            // 使用注入的边界
        );
        if (enableLogging) {
            logger.info("网格构建完成，耗时：{} ms", System.currentTimeMillis() - gridStart);
        }

        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();

        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        AtomicInteger processedCount = new AtomicInteger(0);
        long filterStart = System.currentTimeMillis();

        allTaxiIds.parallelStream().forEach(taxiId -> {
            long startNano = System.nanoTime();

            // 获取数据
            NavigableMap<LocalDateTime, TaxiRecord> records = taxiRepository.getRecordsByTaxiId(taxiId);
            if (records == null) return;
            NavigableMap<LocalDateTime, TaxiRecord> filtered = records.subMap(startTime, true, endTime, true);

            // 使用 Set 记录本出租车在某个时间槽访问过哪些格子，防止重复计数
            Map<LocalDateTime, Set<String>> visitedCellsInTimeSlot = new HashMap<>();

            for (TaxiRecord record : filtered.values()) {
                GridCell cell = grid.getCellByPosition(record.getLongitude(), record.getLatitude());
                if (cell == null) continue; //跳过格子外的点

                LocalDateTime timeSlot = roundToTimeSlot(record.getTimestamp(), timeSlotMinutes);
                String cellKey = cell.getRow() + "," + cell.getCol();

                // 检查该出租车在该时间槽是否已访问过此格子
                Set<String> visitedKeys = visitedCellsInTimeSlot.computeIfAbsent(timeSlot, k -> new HashSet<>());
                if (visitedKeys.add(cellKey)) { // 如果是第一次访问该格子 (Set.add 返回 true)
                    // 更新密度计数 (线程安全)
                    densityData.computeIfAbsent(timeSlot, ts -> new ConcurrentHashMap<>())
                            .computeIfAbsent(cellKey, ck -> new AtomicInteger(0))
                            .incrementAndGet();
                }
            }
            long endNano = System.nanoTime();
            long costMillis = (endNano - startNano) / 1_000_000;
            if (costMillis > 5000 && enableLogging) {
                logger.warn("出租车 {} 数据处理耗时过长：{} ms", taxiId, costMillis);
            }
            int current = processedCount.incrementAndGet();
            if (current % 1000 == 0 && enableLogging) {
                long elapsed = System.currentTimeMillis() - filterStart;
                logger.info("已处理出租车数: {}，当前耗时: {} ms", current, elapsed);
            }
        });

        if (enableLogging) {
            logger.info("全部出租车数据处理完成，总耗时：{} ms", System.currentTimeMillis() - filterStart);
        }

        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);

        // **** 从 densityData 计算最终密度图 ****
        long densityStart = System.currentTimeMillis();
        Map<LocalDateTime, Map<String, Integer>> densityMap = calculateDensityMapFromData(timeSlots);
        long densityCost = System.currentTimeMillis() - densityStart;
        if (enableLogging) {
            logger.info("密度图计算完成，耗时：{} ms", densityCost);
        }

        int totalGridCount = grid.getAllCells().size();
        int totalTimeSlots = timeSlots.size();
        long resultSize = densityMap.values().stream().mapToInt(Map::size).sum();
        if (enableLogging) {
            logger.info("结果网格数量：{}，时间段数量：{}，总非空单元格记录数：{}", totalGridCount, totalTimeSlots, resultSize);
        }

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        if (enableLogging) {
            logger.info("当前JVM内存使用：{} MB", usedMemoryMB);
        }

        // 传回结果
        DensityAnalysisResult result = new DensityAnalysisResult();
        result.setGridSize(grid.getGridSize());
        result.setMinLat(grid.getMinLat());
        result.setMinLon(grid.getMinLon());
        result.setMaxLat(grid.getMaxLat());
        result.setMaxLon(grid.getMaxLon());
        result.setRows(grid.getRows());
        result.setCols(grid.getCols());
        result.setTimeSlots(timeSlots);
        result.setDensityMap(densityMap);

        if (resultCache.size() > 100) {
            String keyToRemove = resultCache.keySet().iterator().next();
            resultCache.remove(keyToRemove);
        }
        resultCache.put(cacheKey, result);

        long globalCost = System.currentTimeMillis() - globalStart;
        if (enableLogging) {
            logger.info("车流密度分析完成，总耗时：{} ms", globalCost);
        }
        return result;
    }

    private String generateCacheKey(DensityQuery query) {
        return String.format("%f:%s:%s:%d",
                query.getGridSize(),
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

    /**
     * **** 从内部维护的 densityData 计算最终的密度图 ****
     *
     * @param timeSlots 需要计算的时间槽列表.
     * @return 最终的密度图 Map<LocalDateTime, Map<String, Integer>>.
     */
    private Map<LocalDateTime, Map<String, Integer>> calculateDensityMapFromData(List<LocalDateTime> timeSlots) {
        Map<LocalDateTime, Map<String, Integer>> finalMap = new HashMap<>();
        long start = System.currentTimeMillis();
        for (LocalDateTime timeSlot : timeSlots) {
            long slotStart = System.nanoTime();
            // 从 densityData 获取该时间槽的数据
            Map<String, AtomicInteger> cellCounts = densityData.get(timeSlot);
            Map<String, Integer> cellDensity = new HashMap<>();
            int nonEmptyCells = 0;

            if (cellCounts != null) {
                // 将 AtomicInteger 转换为 Integer
                for (Map.Entry<String, AtomicInteger> entry : cellCounts.entrySet()) {
                    int density = entry.getValue().get();
                    if (density > 0) { // 理论上这里应该都大于0
                        cellDensity.put(entry.getKey(), density);
                        nonEmptyCells++;
                    }
                }
            }
            // 即使没有数据也要放入空的 Map，保持时间槽完整性
            finalMap.put(timeSlot, cellDensity);

            // 耗时日志 (逻辑不变)
            long slotCostMs = (System.nanoTime() - slotStart) / 1_000_000;
            if (slotCostMs > 100 && enableLogging) {
                logger.warn("时间段 {} 密度图转换耗时较长：{} ms，非空单元格数：{}", timeSlot, slotCostMs, nonEmptyCells);
            }
        }
        long total = System.currentTimeMillis() - start;
        if (enableLogging) logger.info("calculateDensityMapFromData 总耗时：{} ms，时间段数：{}", total, timeSlots.size());
        return finalMap;
    }

    public void clearCache() {
        resultCache.clear();
        if (enableLogging) {
            logger.info("缓存已清空");
        }
    }
}
