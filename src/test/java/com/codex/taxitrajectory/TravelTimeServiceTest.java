package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.TravelTimeQuery;
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.service.TravelTimeService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles; // 根据需要启用

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * {@link TravelTimeService} 的集成测试类。
 * <p>
 * 该测试类旨在验证F9通行时间分析模块的功能和性能。
 * 它包括预热运行、针对不同时间跨度的性能测试，并打印结果摘要。
 * </p>
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // 按 @Order 注解顺序执行测试
class TravelTimeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeServiceTest.class);

    @Autowired
    private TravelTimeService travelTimeService;

    // 测试用的区域 A 和 B
    private static Region regionA;
    private static Region regionB;

    // 定义两个具体的测试时间跨度
    private static LocalDateTime shortSpanStart; // 短时间跨度 - 开始
    private static LocalDateTime shortSpanEnd;   // 短时间跨度 - 结束
    private static LocalDateTime longSpanStart;  // 长时间跨度 - 开始
    private static LocalDateTime longSpanEnd;    // 长时间跨度 - 结束

    // 日期时间格式化器，用于日志输出
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    // 控制是否在日志中打印完整轨迹的开关
    private static final boolean PRINT_FULL_TRAJECTORY = true; // 设置为 true 打印完整轨迹，false 则不打印

    /**
     * 在所有测试方法执行前运行一次，用于初始化测试数据。
     */
    @BeforeAll
    static void setUpClass() {
        log.info("初始化测试环境：设置测试区域和具体时间跨度...");

        // 区域 A: 例如望京区域 (经度约 116.45 - 116.50, 纬度约 40.00 - 40.05)
        regionA = new Region(116.45, 40.00, 116.50, 40.05);
        log.info("测试区域 A (例如望京) 已设置: {}", regionA);

        // 区域 B: 例如国贸 CBD 区域 (经度约 116.44 - 116.48, 纬度约 39.90 - 39.92)
        regionB = new Region(116.44, 39.90, 116.48, 39.92);
        log.info("测试区域 B (例如国贸) 已设置: {}", regionB);

        // 定义具体的短时间跨度 (例如：某个工作日上午的繁忙2小时)
        // 请确保这些时间与您的测试数据集中的时间范围相匹配
        shortSpanStart = LocalDateTime.of(2008, 2, 3, 8, 0, 0); // 年, 月, 日, 时, 分, 秒
        shortSpanEnd = LocalDateTime.of(2008, 2, 3, 10, 0, 0);
        log.info("短时间跨度已设置: 从 {} 到 {}", shortSpanStart.format(dtf), shortSpanEnd.format(dtf));

        // 定义具体的长时间跨度 (例如：某个完整的3天)
        longSpanStart = LocalDateTime.of(2008, 2, 2, 0, 0, 0);
        longSpanEnd = LocalDateTime.of(2008, 2, 4, 23, 59, 59);
        log.info("长时间跨度已设置: 从 {} 到 {}", longSpanStart.format(dtf), longSpanEnd.format(dtf));

        log.info("测试环境初始化完成。");
    }

    /**
     * 预热测试：执行一次查询以确保服务和缓存等已初始化。
     * 其结果通常不用于最终的性能评估。
     */
    @Test
    @Order(1) // 第一个执行
    @DisplayName("预热运行 (结果通常忽略，用于初始化)")
    void warmUp() {
        log.info("开始预热运行...");
        // 使用短时间跨度进行预热，查询条件与实际测试类似
        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, shortSpanStart, shortSpanEnd);

        Instant startTime = Instant.now();
        TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
        Instant endTime = Instant.now();
        log.info("预热运行耗时 {} 毫秒。", Duration.between(startTime, endTime).toMillis());

        Assertions.assertNotNull(result, "预热运行的结果不应为 null。");
        // 可以根据预热时段是否有预期结果，选择性添加更具体的断言，例如：
        // if (expectedToFindPathDuringWarmup) {
        //     Assertions.assertTrue(result.isFound(), "预热期间应找到路径。");
        // } else {
        //     Assertions.assertFalse(result.isFound(), "预热期间不应找到路径。");
        // }
        log.info("预热运行完成。");

        try {
            TimeUnit.SECONDS.sleep(1); // 短暂暂停，让系统稳定或日志刷盘
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("预热后的等待被中断。", e);
        }
    }


    /**
     * 性能测试：针对定义的“短时间跨度”进行多次查询，并计算平均执行时间。
     */
    @Test
    @Order(2) // 第二个执行
    @DisplayName("性能测试：短时间跨度查询 (例如2小时)")
    void  testPerformance_ShortSpanQuery() {
        log.info("开始性能测试 - 短时间跨度查询 (从 {} 到 {})", shortSpanStart.format(dtf), shortSpanEnd.format(dtf));

        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, shortSpanStart, shortSpanEnd);
        int numberOfRuns = 3; // 执行多次运行以获取更稳定的平均性能数据
        long totalDurationMillis = 0;

        for (int i = 1; i <= numberOfRuns; i++) {
            log.info("执行短跨度查询，第 {}/{} 次运行...", i, numberOfRuns);
            Instant startTime = Instant.now();
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
            Instant endTime = Instant.now();
            long currentRunDurationMillis = Duration.between(startTime, endTime).toMillis();
            totalDurationMillis += currentRunDurationMillis;
            log.info("第 {} 次运行完成，耗时 {} 毫秒。", i, currentRunDurationMillis);

            log.info("  --- 第 {} 次运行结果摘要 ---", i);
            printSingleResultSummary(shortSpanStart, shortSpanEnd, result);
            log.info("  --- 结果摘要结束 ---");

            Assertions.assertNotNull(result, "短跨度查询结果不应为 null。");
            // 根据您的数据集和查询条件，添加更具体的断言，例如：
            // Assertions.assertTrue(result.isFound(), "短时间跨度查询应找到有效路径。");
        }

        double averageDurationMillis = (double) totalDurationMillis / numberOfRuns;
        log.info("--- 短时间跨度查询性能总结 ---");
        log.info("查询时间范围: 从 {} 到 {}", shortSpanStart.format(dtf), shortSpanEnd.format(dtf));
        log.info("执行次数: {}", numberOfRuns);
        log.info("平均执行时间: {} 毫秒", String.format("%.2f", averageDurationMillis));
        log.info("------------------------------------------------------");
    }

    /**
     * 性能测试：针对定义的“长时间跨度”进行多次查询，并计算平均执行时间。
     */
    @Test
    @Order(3) // 第三个执行
    @DisplayName("性能测试：长时间跨度查询 (例如3天)")
    void testPerformance_LongSpanQuery() {
        log.info("开始性能测试 - 长时间跨度查询 (从 {} 到 {})", longSpanStart.format(dtf), longSpanEnd.format(dtf));

        TravelTimeQuery query = new TravelTimeQuery(regionA, regionB, longSpanStart, longSpanEnd);
        int numberOfRuns = 3; // 长时间查询通常更耗时，运行次数可以适当减少
        long totalDurationMillis = 0;

        for (int i = 1; i <= numberOfRuns; i++) {
            log.info("执行长跨度查询，第 {}/{} 次运行...", i, numberOfRuns);
            Instant startTime = Instant.now();
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);
            Instant endTime = Instant.now();
            long currentRunDurationMillis = Duration.between(startTime, endTime).toMillis();
            totalDurationMillis += currentRunDurationMillis;
            log.info("第 {} 次运行完成，耗时 {} 毫秒。", i, currentRunDurationMillis);

            log.info("  --- 第 {} 次运行结果摘要 ---", i);
            printSingleResultSummary(longSpanStart, longSpanEnd, result);
            log.info("  --- 结果摘要结束 ---");

            Assertions.assertNotNull(result, "长跨度查询结果不应为 null。");
            // 根据您的数据集和查询条件，添加更具体的断言
            // Assertions.assertTrue(result.isFound(), "长时间跨度查询应找到有效路径。");
        }

        double averageDurationMillis = (double) totalDurationMillis / numberOfRuns;
        log.info("--- 长时间跨度查询性能总结 ---");
        log.info("查询时间范围: 从 {} 到 {}", longSpanStart.format(dtf), longSpanEnd.format(dtf));
        log.info("执行次数: {}", numberOfRuns);
        log.info("平均执行时间: {} 毫秒", String.format("%.2f", averageDurationMillis));
        log.info("------------------------------------------------------");
    }

    /**
     * 辅助方法：打印单个 {@link TravelTimeResult} 对象的摘要信息。
     *
     * @param queryStartTime 查询的开始时间 (用于日志)。
     * @param queryEndTime   查询的结束时间 (用于日志)。
     * @param result         从服务获取的 {@link TravelTimeResult} 对象。
     */
    private void printSingleResultSummary(LocalDateTime queryStartTime, LocalDateTime queryEndTime, TravelTimeResult result) {
        String startTimeStr = queryStartTime.format(dtf);
        String endTimeStr = queryEndTime.format(dtf);

        if (result != null && result.isFound()) {
            Duration minDuration = result.getMinTravelTime();
            long minutes = minDuration.toMinutes();
            long secondsPart = minDuration.toSecondsPart(); // 获取秒的部分 (0-59)
            List<TaxiRecord> shortestPath = result.getShortestPath();
            int pathPointCount = (shortestPath != null) ? shortestPath.size() : 0;

            log.info("    时间段 [{} - {}]: 找到路径 ✔ | 最短耗时: {} 分 {} 秒 | 轨迹点数: {}",
                    startTimeStr, endTimeStr, minutes, secondsPart, pathPointCount);

            if (PRINT_FULL_TRAJECTORY && shortestPath != null && !shortestPath.isEmpty()) {
                log.info("      --- 完整轨迹详情 (共 {} 个点) ---", pathPointCount);
                for (int i = 0; i < shortestPath.size(); i++) {
                    TaxiRecord point = shortestPath.get(i);
                    if (point != null) {
                        String timestampStr = (point.getTimestamp() != null) ? point.getTimestamp().format(dtf) : "时间戳N/A";
                        // 经纬度为 Double 类型，如果为 null 会打印 "null"
                        log.info("        轨迹点 {}: 时间={}, 经度={}, 纬度={}",
                                (i + 1), timestampStr, point.getLongitude(), point.getLatitude());
                    } else {
                        log.warn("        轨迹点 {}: 警告 - 轨迹中发现 null 的 TaxiRecord 对象！", (i + 1));
                    }
                }
                log.info("      --- 完整轨迹详情结束 ---");
            }
        } else {
            log.info("    时间段 [{} - {}]: 未找到有效路径 ❌ (结果为 null 或 isFound()=false)", startTimeStr, endTimeStr);
        }
    }
}
