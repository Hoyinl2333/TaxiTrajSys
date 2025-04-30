package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.TravelTimeQuery;
// 注意：这里导入的是你重命名后的 TravelTimeResult，它包含了路径和最短时间信息
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.service.TravelTimeService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List; // 仍然需要 List 用于 ShortestPathTrajectory
import java.util.concurrent.TimeUnit;


@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TravelTimeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeServiceTest.class);

    @Autowired
    private TravelTimeService travelTimeService;

    private static Region regionA;
    private static Region regionB;

    // 定义两个具体的测试时间跨度
    private static LocalDateTime shortSpanStart;
    private static LocalDateTime shortSpanEnd;
    private static LocalDateTime longSpanStart;
    private static LocalDateTime longSpanEnd;


    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    @BeforeAll
    static void setUpClass() {
        log.info("设置测试区域和具体时间跨度...");
        // 区域 A: 望京区域 (经度约 116.45 - 116.50, 纬度约 40.00 - 40.05)
        regionA = new Region(116.45, 40.00, 116.50, 40.05);
        log.info("设置区域 A (望京): {}", regionA);

        // 区域 B: 国贸 CBD 区域 (经度约 116.44 - 116.48, 纬度约 39.90 - 39.92)
        regionB = new Region(116.44, 39.90, 116.48, 39.92);
        log.info("设置区域 B (国贸): {}", regionB);

        // 定义具体的短时间跨度 (例如：某个繁忙的2小时)
        shortSpanStart = LocalDateTime.of(2008, 2, 3, 8, 0);
        shortSpanEnd = LocalDateTime.of(2008, 2, 3, 10, 0);
        log.info("设置短时间跨度: {} 到 {}", shortSpanStart.format(dtf), shortSpanEnd.format(dtf));


        // 定义具体的长时间跨度 (例如：某个完整的3天)
        longSpanStart = LocalDateTime.of(2008, 2, 2, 0, 0);
        longSpanEnd = LocalDateTime.of(2008, 2, 4, 23, 59, 59);
        log.info("设置长时间跨度: {} 到 {}", longSpanStart.format(dtf), longSpanEnd.format(dtf));


        log.info("设置完成.");
    }

    @Test
    @Order(1)
    @DisplayName("预热运行 (结果忽略)")
    void warmUp() {
        log.info("开始预热运行...");
        // 可以使用短时间跨度进行预热
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, shortSpanStart, shortSpanEnd);

        Instant start = Instant.now();
        // 调用分析方法，返回单个 TravelTimeResult
        TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
        Instant end = Instant.now();
        log.info("预热运行在 {} 毫秒内完成", Duration.between(start, end).toMillis());

        // 简单的断言
        Assertions.assertNotNull(result, "预热结果不应为 null");
        // 根据预热时段是否有预期结果，可以添加 result.isFound() 的断言

        try {
            TimeUnit.SECONDS.sleep(2); // 短暂暂停
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @Test
    @Order(2)
    @DisplayName("性能测试：短时间跨度查询")
    void testPerformance_ShortSpanQuery() {
        log.info("开始性能测试：短时间跨度查询 ({} 到 {})", shortSpanStart.format(dtf), shortSpanEnd.format(dtf));

        // 创建针对短时间跨度的单一查询
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, shortSpanStart, shortSpanEnd);

        long totalDurationMillis = 0;
        int runs = 5; // 执行多次运行取平均

        for (int i = 1; i <= runs; i++) {
            log.info("执行短跨度查询运行 {}/{}...", i, runs);
            Instant start = Instant.now();
            // 调用服务分析
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
            Instant end = Instant.now();
            long durationMillis = Duration.between(start, end).toMillis();
            totalDurationMillis += durationMillis;
            log.info("运行 {} 在 {} 毫秒内完成。", i, durationMillis);

            // --- 打印本次运行结果摘要 ---
            log.info("  --- 运行 {} 结果摘要 ---", i);
            printSingleResultSummary(shortSpanStart, shortSpanEnd, result); // 调用辅助方法打印单个结果
            log.info("  --- 结果摘要结束 ---");
            // --- 结果打印结束 ---

            // 断言：检查结果是否有效
            Assertions.assertNotNull(result, "结果不应为 null");
            // 根据预期的结果，可以添加 result.isFound() 的断言，例如：
            // Assertions.assertTrue(result.isFound(), "短时间跨度查询应找到结果");
        }

        // 计算并打印平均执行时间
        double averageDurationMillis = (double) totalDurationMillis / runs;
        log.info("--- 性能结果 (短时间跨度查询) ---");
        log.info("查询时间范围: {} 到 {}", shortSpanStart.format(dtf), shortSpanEnd.format(dtf));
        log.info("在 {} 次运行中的平均执行时间: {} 毫秒", runs, String.format("%.2f", averageDurationMillis));
        log.info("------------------------------------------------------");
    }

    @Test
    @Order(3)
    @DisplayName("性能测试：长时间跨度查询")
    void testPerformance_LongSpanQuery() {
        log.info("开始性能测试：长时间跨度查询 ({} 到 {})", longSpanStart.format(dtf), longSpanEnd.format(dtf));

        // 创建针对长时间跨度的单一查询
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, longSpanStart, longSpanEnd);

        long totalDurationMillis = 0;
        int runs = 3; // 长跨度运行次数可以少一些

        for (int i = 1; i <= runs; i++) {
            log.info("执行长跨度查询运行 {}/{}...", i, runs);
            Instant start = Instant.now();
            // 调用服务分析
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
            Instant end = Instant.now();
            long durationMillis = Duration.between(start, end).toMillis();
            totalDurationMillis += durationMillis;
            log.info("运行 {} 在 {} 毫秒内完成。", i, durationMillis);

            // --- 打印本次运行结果摘要 ---
            log.info("  --- 运行 {} 结果摘要 ---", i);
            printSingleResultSummary(longSpanStart, longSpanEnd, result); // 调用辅助方法打印单个结果
            log.info("  --- 结果摘要结束 ---");
            // --- 结果打印结束 ---

            // 断言：检查结果是否有效
            Assertions.assertNotNull(result, "结果不应为 null");
            // 根据预期的结果，可以添加 result.isFound() 的断言
            // Assertions.assertTrue(result.isFound(), "长时间跨度查询应找到结果");
        }

        // 计算并打印平均执行时间
        double averageDurationMillis = (double) totalDurationMillis / runs;
        log.info("--- 性能结果 (长时间跨度查询) ---");
        log.info("查询时间范围: {} 到 {}", longSpanStart.format(dtf), longSpanEnd.format(dtf));
        log.info("在 {} 次运行中的平均执行时间: {} 毫秒", runs, String.format("%.2f", averageDurationMillis));
        log.info("------------------------------------------------------");
    }

    // 控制是否打印完整轨迹 (现在直接在这里使用，或者传入辅助方法)
    private static final boolean PRINT_FULL_TRAJECTORY = true; // <<-- 在这里切换 true/false

    /**
     * 辅助方法：打印单个 TravelTimeResult 的信息摘要。
     *
     * @param queryStartTime 查询的开始时间
     * @param queryEndTime   查询的结束时间
     * @param result         分析返回的 TravelTimeResult 对象
     */
    private void printSingleResultSummary(LocalDateTime queryStartTime, LocalDateTime queryEndTime, TravelTimeResult result) {
        String startTimeStr = queryStartTime.format(dtf);
        String endTimeStr = queryEndTime.format(dtf);

        if (result != null && result.isFound()) {
            Duration duration = result.getMinTravelTime();
            long minutes = duration.toMinutes();
            long seconds = duration.toSecondsPart();
            List<TaxiRecord> path = result.getShortestPath(); // 获取路径列表
            int pathSize = (path != null) ? path.size() : 0;

            log.info("    时间段 [{} - {}]: 找到路径 ✔ | 最短耗时: {} 分 {} 秒 | 轨迹点数: {}",
                    startTimeStr, endTimeStr, minutes, seconds, pathSize);

            // --- 根据开关决定是否打印完整轨迹 ---
            if (PRINT_FULL_TRAJECTORY && path != null && !path.isEmpty()) {
                log.info("      --- 完整轨迹点 ({} 个) ---", pathSize);
                for (int i = 0; i < path.size(); i++) {
                    TaxiRecord point = path.get(i);
                    if (point != null) {
                        // 检查时间戳是否为 null 和格式化
                        String timestampStr = point.getTimestamp() != null ? point.getTimestamp().format(dtf) : "N/A";
                        log.info("        点 {}: Time={}, Lon={}, Lat={}",
                                i + 1, timestampStr, point.getLongitude(), point.getLatitude());
                    } else {
                        log.warn("        点 {}: 轨迹中发现 null 记录！", i + 1);
                    }
                }
                log.info("      --- 完整轨迹点结束 ---");
            }
            // --- 打印轨迹结束 ---

        } else {
            log.info("    时间段 [{} - {}]: 未找到路径 ❌", startTimeStr, endTimeStr);
        }
    }
}