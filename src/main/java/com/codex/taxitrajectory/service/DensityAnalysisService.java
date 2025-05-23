package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.Grid;
import com.codex.taxitrajectory.model.core.GridCell;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.model.result.DensityAnalysisResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
// import com.codex.taxitrajectory.utils.GeoUtils; // GeoUtils 未在本服务中使用
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class DensityAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(DensityAnalysisService.class);

    @Value("${logging.service.enabled:true}")
    private boolean enableLogging; // 日志开关，可以通过配置文件控制详细日志的输出

    private final TaxiRepository taxiRepository;

//    // 全局地图边界配置 (现在仅作为参考或潜在的系统级校验，网格主要由查询参数定义)
//    // 如果完全不再使用，可以考虑移除
//    @Value("${map.bounds.minLongitude}")
//    private double configMapMinLon;
//    @Value("${map.bounds.minLatitude}")
//    private double configMapMinLat;
//    @Value("${map.bounds.maxLongitude}")
//    private double configMapMaxLon;
//    @Value("${map.bounds.maxLatitude}")
//    private double configMapMaxLat;

    @Autowired
    public DensityAnalysisService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    public DensityAnalysisResult analyzeTrafficDensity(DensityQuery query) {
        long globalStartTimeMs = System.currentTimeMillis();
//        logger.info("======================");
//        logger.info("开始处理车流密度分析请求. 查询参数: {}", query);

        // 1. 参数校验 (在 DensityQuery 内部或此处进行)
        // DensityQuery 的 @Valid 会触发 JSR 303 注解校验

        // 2. 创建网格 (使用查询参数中用户定义的边界)
        // 注意: DensityQuery 需已更新，包含 minLongitude, minLatitude, maxLongitude, maxLatitude 字段
        logger.info("使用用户定义的地理边界进行网格构建: MinLon={}, MinLat={}, MaxLon={}, MaxLat={}, GridSizeKm={}",
                query.getMinLongitude(), query.getMinLatitude(), query.getMaxLongitude(), query.getMaxLatitude(), query.getGridSize());

        long gridBuildStartMs = System.currentTimeMillis();
        Grid grid = new Grid(
                query.getGridSize(),
                query.getMinLongitude(),
                query.getMinLatitude(),
                query.getMaxLongitude(),
                query.getMaxLatitude()
        );
        if (enableLogging) {
            logger.info("网格构建完成. 行数: {}, 列数: {}. 耗时: {} ms", grid.getRows(), grid.getCols(), (System.currentTimeMillis() - gridBuildStartMs));
        }

        // 3. 初始化用于存储当前请求密度数据的局部变量 (线程安全)
        ConcurrentHashMap<LocalDateTime, ConcurrentHashMap<String, AtomicInteger>> currentRequestDensityData = new ConcurrentHashMap<>();

        LocalDateTime queryStartTime = query.getStartTime();
        LocalDateTime queryEndTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();

        // 4. 获取所有出租车ID并进行并行处理
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();
        if (allTaxiIds == null || allTaxiIds.isEmpty()) {
            logger.warn("未找到任何出租车ID，无法进行密度分析。");
            // 返回一个表示没有数据的空结果
            return createEmptyResult(query, grid, generateTimeSlots(queryStartTime, queryEndTime, timeSlotMinutes));
        }
        logger.info("开始处理 {} 辆出租车的轨迹数据...", allTaxiIds.size());

        AtomicInteger processedTaxiCount = new AtomicInteger(0);
        long dataProcessingStartMs = System.currentTimeMillis();

        allTaxiIds.parallelStream().forEach(taxiId -> {
            long taxiProcessStartNano = System.nanoTime();
            // 用于跟踪本地图内每个时间槽访问过的格子，防止重复计数
            Map<LocalDateTime, Set<String>> visitedCellsInTimeSlotForTaxi = new HashMap<>();

            try (Stream<TaxiRecord> recordStream = taxiRepository.streamRecordsByTaxiId(taxiId)) {
                if (recordStream == null) {
                    if (enableLogging) logger.debug("出租车 {} 的记录流为空.", taxiId);
                    return; // 跳过此出租车
                }

                recordStream
                        .filter(record -> record != null && record.getTimestamp() != null &&
                                !record.getTimestamp().isBefore(queryStartTime) && // 时间范围过滤
                                !record.getTimestamp().isAfter(queryEndTime))
                        .forEach(record -> {
                            GridCell cell = grid.getCellByPosition(record.getLongitude(), record.getLatitude());
                            if (cell == null) { // 跳过网格区域外的点
                                // logger.trace("记录点 ({},{}) 在网格区域外，已跳过.", record.getLongitude(), record.getLatitude());
                                return;
                            }

                            LocalDateTime timeSlot = roundToTimeSlot(record.getTimestamp(), timeSlotMinutes);
                            String cellKey = cell.getRow() + "," + cell.getCol();

                            Set<String> visitedKeys = visitedCellsInTimeSlotForTaxi.computeIfAbsent(timeSlot, k -> new HashSet<>());
                            if (visitedKeys.add(cellKey)) { // 如果该出租车在该时间槽第一次访问此格子
                                currentRequestDensityData
                                        .computeIfAbsent(timeSlot, ts -> new ConcurrentHashMap<>())
                                        .computeIfAbsent(cellKey, ck -> new AtomicInteger(0))
                                        .incrementAndGet();
                            }
                        });

            } catch (IOException e) {
                logger.error("处理出租车 {} 的数据流时发生IO错误: {}", taxiId, e.getMessage(), e);
                // 根据策略决定是否需要中断整个分析或仅跳过此出租车
            } catch (Exception e) {
                logger.error("处理出租车 {} 数据时发生未知异常: {}", taxiId, e.getMessage(), e);
            }

            long taxiProcessEndNano = System.nanoTime();
            long costMillis = (taxiProcessEndNano - taxiProcessStartNano) / 1_000_000;

            if (enableLogging && costMillis > 5000) { // 记录处理时间过长的出租车
                logger.warn("出租车 {} 数据处理耗时较长: {} ms", taxiId, costMillis);
            }

            int currentCount = processedTaxiCount.incrementAndGet();
            if (enableLogging && currentCount % 1000 == 0) {
                logger.info("已处理出租车数量: {} / {}, 当前耗时: {} ms",
                        currentCount, allTaxiIds.size(), (System.currentTimeMillis() - dataProcessingStartMs));
            }
        });

        if (enableLogging) {
            logger.info("全部出租车数据流处理完成. 总耗时: {} ms", (System.currentTimeMillis() - dataProcessingStartMs));
        }

        // 5. 生成时间槽列表并计算最终密度图
        List<LocalDateTime> timeSlots = generateTimeSlots(queryStartTime, queryEndTime, timeSlotMinutes);
        logger.info("已生成 {} 个时间槽.", timeSlots.size());

        long densityMapCalcStartMs = System.currentTimeMillis();
        Map<LocalDateTime, Map<String, Integer>> finalDensityMap = calculateDensityMapFromLocalData(timeSlots, currentRequestDensityData);
        if (enableLogging) {
            logger.info("最终密度图计算完成. 耗时: {} ms", (System.currentTimeMillis() - densityMapCalcStartMs));
        }

        // 6. 组装并返回结果
        DensityAnalysisResult result = new DensityAnalysisResult();
        result.setRows(grid.getRows());
        result.setCols(grid.getCols());
        result.setTimeSlots(timeSlots);
        result.setDensityMap(finalDensityMap);

        if (enableLogging) {
            long totalGridCells = (long)grid.getRows() * grid.getCols();
            long nonEmptyCellRecords = finalDensityMap.values().stream().mapToLong(Map::size).sum();
            logger.info("分析结果统计: 总网格单元数={}, 时间段数量={}, 总非空单元格记录数={}",
                    totalGridCells, timeSlots.size(), nonEmptyCellRecords);
        }

        logger.info("车流密度分析成功完成. 总耗时: {} ms", (System.currentTimeMillis() - globalStartTimeMs));
        logger.info("======================");
        return result;
    }

    private List<LocalDateTime> generateTimeSlots(LocalDateTime start, LocalDateTime end, int intervalMinutes) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            slots.add(current);
            current = current.plusMinutes(intervalMinutes);
        }
        return slots;
    }

    private LocalDateTime roundToTimeSlot(LocalDateTime timestamp, int minutes) {
        if (minutes <= 0) { // 防御性编程，避免除零错误
            logger.warn("时间槽分钟数无效 ({})，将返回原始时间戳的不规整时间槽。", minutes);
            return timestamp.withSecond(0).withNano(0);
        }
        int minuteOfHour = timestamp.getMinute();
        int roundedMinute = (minuteOfHour / minutes) * minutes;
        return timestamp.withMinute(roundedMinute).withSecond(0).withNano(0);
    }

    private Map<LocalDateTime, Map<String, Integer>> calculateDensityMapFromLocalData(
            List<LocalDateTime> timeSlots,
            Map<LocalDateTime, ConcurrentHashMap<String, AtomicInteger>> localDensityData) {

        if (enableLogging) logger.info("开始从局部密度数据转换最终密度图...");
        Map<LocalDateTime, Map<String, Integer>> finalMap = new HashMap<>();
        long conversionStartMs = System.currentTimeMillis();

        for (LocalDateTime timeSlot : timeSlots) {
            Map<String, AtomicInteger> cellCountsForTimeSlot = localDensityData.get(timeSlot);
            Map<String, Integer> cellDensity = new HashMap<>();
            int nonEmptyCellsInSlot = 0;

            if (cellCountsForTimeSlot != null && !cellCountsForTimeSlot.isEmpty()) {
                for (Map.Entry<String, AtomicInteger> entry : cellCountsForTimeSlot.entrySet()) {
                    int density = entry.getValue().get();
                    if (density > 0) {
                        cellDensity.put(entry.getKey(), density);
                        nonEmptyCellsInSlot++;
                    }
                }
            }
            finalMap.put(timeSlot, cellDensity); // 即使cellDensity为空map，也为该时间槽放入记录

            if (enableLogging && nonEmptyCellsInSlot > 0) {
                logger.debug("时间槽 [{}] 转换后包含 {} 个非空密度单元.", timeSlot, nonEmptyCellsInSlot);
            }
        }
        if (enableLogging) {
            logger.info("最终密度图转换总耗时: {} ms. 处理时间槽数: {}", (System.currentTimeMillis() - conversionStartMs), timeSlots.size());
        }
        return finalMap;
    }

    // 辅助方法，用于在没有数据时返回一个空的、但结构完整的Result对象
    private DensityAnalysisResult createEmptyResult(DensityQuery query, Grid grid, List<LocalDateTime> timeSlots) {
        logger.info("创建空的密度分析结果，因为未找到可处理的出租车数据或处理过程中无有效密度数据。");
        DensityAnalysisResult emptyResult = new DensityAnalysisResult();
        emptyResult.setRows(grid.getRows());
        emptyResult.setCols(grid.getCols());
        emptyResult.setTimeSlots(timeSlots);

        Map<LocalDateTime, Map<String, Integer>> emptyDensityMap = new HashMap<>();
        for (LocalDateTime slot : timeSlots) {
            emptyDensityMap.put(slot, Collections.emptyMap());
        }
        emptyResult.setDensityMap(emptyDensityMap);
        return emptyResult;
    }
}