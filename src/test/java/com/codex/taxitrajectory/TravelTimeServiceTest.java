package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.core.TimeInterval;
import com.codex.taxitrajectory.model.query.TravelTimeQuery;
import com.codex.taxitrajectory.model.result.ShortestPathInfo;
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.service.TravelTimeService;
import org.junit.jupiter.api.*; // 使用 JUnit 5 导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; // 如果使用 Spring Boot 上下文
import org.springframework.test.context.ActiveProfiles; // 可选：如果有特定的测试配置文件

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // 用于格式化日期时间
import java.util.ArrayList; // 需要导入 ArrayList
import java.util.Comparator;
import java.util.List;
import java.util.Map; // 导入 Map
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// --- 选项 1: Spring Boot 集成测试 ---
@SpringBootTest // 加载完整的应用程序上下文
@ActiveProfiles("test") // 可选：使用 'test' application.yml/properties
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // 如果需要，控制测试执行顺序
class TravelTimeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeServiceTest.class);

    @Autowired
    private TravelTimeService travelTimeService;

    @Autowired
    private TaxiRepository taxiRepository; // 注入 repository 以便触发预加载或缓存检查

    private static Region regionA;
    private static Region regionB;
    private static List<TimeInterval> hourlyIntervals_OneDay;
    private static List<TimeInterval> dailyIntervals_ThreeDays;

    // 定义日期时间格式化器，方便阅读
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");


    @BeforeAll
    static void setUpClass() {
        log.info("设置测试区域和时间间隔...");
        // 区域 A: 望京区域 (可以根据需要微调或扩大范围)
        // 经度约 116.45 - 116.50, 纬度约 40.00 - 40.05
        regionA = new Region(116.45, 40.00, 116.50, 40.05);
        log.info("设置区域 A (望京): {}", regionA); // 打印确认

        // 区域 B: 国贸 CBD 区域 (可以根据需要微调或扩大范围)
        // 经度约 116.44 - 116.48, 纬度约 39.90 - 39.92
        regionB = new Region(116.44, 39.90, 116.48, 39.92);
        log.info("设置区域 B (国贸): {}", regionB); // 打印确认

        // 定义时间间隔
        LocalDateTime dayStart = LocalDateTime.of(2008, 2, 3, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2008,2, 3, 23, 59, 59);
        hourlyIntervals_OneDay = generateIntervals(dayStart, dayEnd, Duration.ofHours(2));

        LocalDateTime multiDayStart = LocalDateTime.of(2008, 2, 2, 0, 0);
        LocalDateTime multiDayEnd = LocalDateTime.of(2008, 2, 4, 23, 59, 59);
        dailyIntervals_ThreeDays = generateIntervals(multiDayStart, multiDayEnd, Duration.ofDays(2));

        log.info("设置完成.");
    }

    @Test
    @Order(1)
    @DisplayName("预热运行 (结果忽略)")
    void warmUp() {
        log.info("开始预热运行...");
        LocalDateTime warmUpStart = LocalDateTime.of(2008, 2, 2, 8, 0);
        LocalDateTime warmUpEnd = LocalDateTime.of(2008, 2, 2, 10, 0);
        TimeInterval warmUpInterval = new TimeInterval(warmUpStart, warmUpEnd);
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, List.of(warmUpInterval));

        Instant start = Instant.now();
        // 调用分析方法，但不打印详细结果
        travelTimeService.analyzeShortestTravelTime(query);
        Instant end = Instant.now();
        log.info("预热运行在 {} 毫秒内完成", Duration.between(start, end).toMillis());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @Test
    @Order(2)
    @DisplayName("性能测试：单日每小时间隔 (带结果打印)")
    void testPerformance_HourlyOneDay() {
        log.info("开始性能测试：单日每小时间隔...");
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, hourlyIntervals_OneDay);

        long totalDurationMillis = 0;
        int runs = 3;

        for (int i = 1; i <= runs; i++) {
            log.info("执行运行 {}/{}...", i, runs);
            Instant start = Instant.now();
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
            Instant end = Instant.now();
            long durationMillis = Duration.between(start, end).toMillis();
            totalDurationMillis += durationMillis;
            log.info("运行 {} 在 {} 毫秒内完成。", i, durationMillis);

            // --- 新增：打印结果摘要 ---
            if (result != null && result.getResults() != null) {
                log.info("  --- 运行 {} 结果摘要 (共 {} 个时间段) ---", i, result.getResults().size());
                printResultSummary(result.getResults()); // 调用辅助方法打印
                log.info("  --- 结果摘要结束 ---");
            } else {
                log.warn("运行 {} 未返回有效结果！", i);
            }
            // --- 结果打印结束 ---

            Assertions.assertNotNull(result, "结果不应为 null");
            Assertions.assertEquals(hourlyIntervals_OneDay.size(), result.getResults().size(), "结果中的间隔数应与查询匹配");
        }

        double averageDurationMillis = (double) totalDurationMillis / runs;
        log.info("--- 性能结果 (每小时间隔 - 单日) ---");
        log.info("在 {} 次运行中的平均执行时间: {} 毫秒", runs, String.format("%.2f", averageDurationMillis));
        log.info("------------------------------------------------------");
    }

    @Test
    @Order(3)
    @DisplayName("性能测试：三日每日间隔 (带结果打印)")
    void testPerformance_DailyThreeDays() {
        log.info("开始性能测试：三日每日间隔...");
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, dailyIntervals_ThreeDays);

        long totalDurationMillis = 0;
        int runs = 3;

        for (int i = 1; i <= runs; i++) {
            log.info("执行运行 {}/{}...", i, runs);
            Instant start = Instant.now();
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
            Instant end = Instant.now();
            long durationMillis = Duration.between(start, end).toMillis();
            totalDurationMillis += durationMillis;
            log.info("运行 {} 在 {} 毫秒内完成。", i, durationMillis);

            // --- 新增：打印结果摘要 ---
            if (result != null && result.getResults() != null) {
                log.info("  --- 运行 {} 结果摘要 (共 {} 个时间段) ---", i, result.getResults().size());
                printResultSummary(result.getResults()); // 调用辅助方法打印
                log.info("  --- 结果摘要结束 ---");
            } else {
                log.warn("运行 {} 未返回有效结果！", i);
            }
            // --- 结果打印结束 ---

            Assertions.assertNotNull(result, "结果不应为 null");
            Assertions.assertEquals(dailyIntervals_ThreeDays.size(), result.getResults().size(), "结果中的间隔数应与查询匹配");
        }

        double averageDurationMillis = (double) totalDurationMillis / runs;
        log.info("--- 性能结果 (每日间隔 - 三日) ---");
        log.info("在 {} 次运行中的平均执行时间: {} 毫秒", runs, String.format("%.2f", averageDurationMillis));
        log.info("------------------------------------------------------");
    }

    // --- 新增控制开关 ---
    // 设置为 true 以打印完整轨迹，设置为 false 则只打印摘要
    private static final boolean PRINT_FULL_TRAJECTORY = true; // <<-- 在这里切换 true/false

    /**
     * 辅助方法：打印 TravelTimeResult 中每个时间段的最短路径信息摘要。
     * 可选打印完整轨迹（通过 PRINT_FULL_TRAJECTORY 控制）。
     *
     * @param shortestPaths Map 包含时间段到最短路径信息的映射
     */
    private void printResultSummary(Map<TimeInterval, ShortestPathInfo> shortestPaths) {
        if (shortestPaths == null || shortestPaths.isEmpty()) {
            log.info("    结果摘要：无数据或结果为空。");
            return;
        }

        final AtomicInteger foundCount = new AtomicInteger(0);
        final AtomicInteger notFoundCount = new AtomicInteger(0);

        List<Map.Entry<TimeInterval, ShortestPathInfo>> sortedEntries = shortestPaths.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(TimeInterval::getStartTime)))
                .toList();

        log.info("    --- 结果摘要详情 (打印完整轨迹: {}) ---", PRINT_FULL_TRAJECTORY); // 提示当前是否打印轨迹

        sortedEntries.forEach(entry -> {
            TimeInterval interval = entry.getKey();
            ShortestPathInfo info = entry.getValue();
            String startTimeStr = interval.getStartTime().format(dtf);
            String endTimeStr = interval.getEndTime().format(dtf);

            if (info != null && info.isFound()) {
                Duration duration = info.getMinTravelTime();
                long minutes = duration.toMinutes();
                long seconds = duration.toSecondsPart();
                List<TaxiRecord> path = info.getShortestPath(); // 获取路径列表
                int pathSize = (path != null) ? path.size() : 0;

                log.info("    时间段 [{} - {}]: 找到路径 ✔ | 最短耗时: {} 分 {} 秒 | 轨迹点数: {}",
                        startTimeStr, endTimeStr, minutes, seconds, pathSize);
                foundCount.incrementAndGet();

                // --- 新增：根据开关决定是否打印完整轨迹 ---
                if (PRINT_FULL_TRAJECTORY && path != null && !path.isEmpty()) {
                    log.info("      --- 完整轨迹点 ({} 个) ---", pathSize);
                    for (int i = 0; i < path.size(); i++) {
                        TaxiRecord point = path.get(i);
                        if (point != null) {
                            log.info("        点 {}: Time={}, Lon={}, Lat={}",
                                    i + 1, // 点序号从 1 开始
                                    point.getTimestamp() != null ? point.getTimestamp().format(dtf) : "N/A", // 检查时间戳是否为 null
                                    point.getLongitude(),
                                    point.getLatitude());
                        } else {
                            log.warn("        点 {}: 轨迹中发现 null 记录！", i + 1);
                        }
                    }
                    log.info("      --- 完整轨迹点结束 ---");
                }
                // --- 打印轨迹结束 ---

            } else {
                log.info("    时间段 [{} - {}]: 未找到路径 ❌", startTimeStr, endTimeStr);
                notFoundCount.incrementAndGet();
            }
        });

        log.info("    --- 摘要统计 ---");
        log.info("    总计时间段: {}", shortestPaths.size());
        log.info("    找到路径的时间段数量: {}", foundCount.get());
        log.info("    未找到路径的时间段数量: {}", notFoundCount.get());
        log.info("    --- 结果摘要结束 ---");
    }



    /**
     * 辅助方法：根据总体开始、结束时间和间隔时长生成时间段列表。
     * (可以放在 TimeInterval 类中或单独的工具类中)
     */
    private static List<TimeInterval> generateIntervals(LocalDateTime overallStart, LocalDateTime overallEnd, Duration intervalDuration) {
        // ... (此方法的实现与之前相同，保持不变) ...
        List<TimeInterval> intervals = new ArrayList<>();
        LocalDateTime currentStart = overallStart;
        while (!currentStart.isAfter(overallEnd)) {
            LocalDateTime currentEnd = currentStart.plus(intervalDuration);
            if (currentEnd.isAfter(overallEnd)) {
                currentEnd = overallEnd;
            }
            if (!currentStart.isAfter(currentEnd)) {
                intervals.add(new TimeInterval(currentStart, currentEnd));
            }
            LocalDateTime nextStart = currentStart.plus(intervalDuration);
            if (nextStart.isAfter(overallEnd) || nextStart.equals(currentStart)) {
                break;
            }
            currentStart = nextStart;
        }
        return intervals;
    }
}