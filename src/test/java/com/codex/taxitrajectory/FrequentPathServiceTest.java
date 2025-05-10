package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.PathFrequency; // 确保 PathFrequency 已更新为包含 List<PointCoordinate>
import com.codex.taxitrajectory.model.core.PointCoordinate; // 引入坐标点类
import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.service.FrequentPathService;

import org.junit.jupiter.api.Disabled; // 保留 @Disabled 注解
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch; // 保留 StopWatch

import java.util.Collections; // 用于可能的空列表处理
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FrequentPathService} 的集成测试类。
 * <p>
 * 该测试类旨在验证F7（全市频繁路径）和F8（区域间频繁路径）分析模块的功能和性能。
 * 测试会处理全量数据，因此可能耗时较长并占用大量内存。
 * </p>
 */
@SpringBootTest
class FrequentPathServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathServiceTest.class);

    @Autowired
    private FrequentPathService frequentPathService;

    // 定义 F8 测试用的区域 (请根据您的实际数据集进行调整以确保有足够的轨迹数据)
    // 示例区域 A: 例如中关村软件园附近
    private final Region regionA = new Region(116.30, 39.97, 116.34, 40.01);
    // 示例区域 B: 例如国贸CBD附近
    private final Region regionB = new Region(116.45, 39.89, 116.49, 39.93);

    /**
     * !!! 警告: 此测试处理全量数据，可能耗时较长并占用大量内存 !!!
     * 运行前请确认JVM堆内存 (-Xmx) 设置是否充足。
     */
    @Test
    @DisplayName("性能测试: F7 - 分析全市频繁路径 (全量数据)")
    //@Disabled("默认禁用此耗时测试，请手动启用以在全量数据上运行性能评估") // 默认禁用，手动启用
    void performanceTest_F7_FullData() {
        logger.warn("!!! 开始 F7 全量数据性能测试，此过程可能耗时较长并占用大量内存 !!!");

        // 1. 定义查询参数
        int k = 10; // 获取 Top 10 频繁路径
        double minDistanceKM = 1; // 路径的最小地理距离为 1 千米
        FrequentPathQuery query = new FrequentPathQuery(k, minDistanceKM);
        logger.info("测试查询参数 (F7 - 全市范围): {}", query);
        logMemoryUsage("F7 分析前"); // 记录分析开始前的内存使用情况

        // 2. 执行分析并使用 StopWatch 计时
        StopWatch stopWatch = new StopWatch("F7 全量数据性能测试");
        FrequentPathResult result = null;
        try {
            stopWatch.start("F7 分析任务");
            result = frequentPathService.analyzeFrequentPaths(query);
            stopWatch.stop();
        } catch (OutOfMemoryError oom) {
            logger.error("!!! F7 性能测试期间发生内存不足错误 (OutOfMemoryError)！请考虑增加 JVM 堆内存 (-Xmx参数) !!!", oom);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("内存溢出后");
            throw oom; // 重新抛出错误，使测试失败1
        } catch (Exception e) {
            logger.error("!!! F7 性能测试期间发生未预期的异常 !!!", e);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("异常发生后");
            throw e; // 重新抛出错误，使测试失败
        } finally {
            // 确保 StopWatch 在任何情况下都会停止（虽然在正常和异常路径中已处理）
            if (stopWatch.isRunning()) {
                stopWatch.stop();
                logger.warn("StopWatch 在 finally 块中被强制停止。");
            }
        }

        // 3. 记录分析结果和性能数据
        logger.info("===== F7 全量数据性能测试结果 =====");
        logger.info("执行总耗时: {} 毫秒 (约 {} 秒)",
                stopWatch.getTotalTimeMillis(),
                String.format("%.2f", stopWatch.getTotalTimeSeconds())); // 格式化秒数

        List<PathFrequency> topPaths = (result != null) ? result.getPathFrequencies() : Collections.emptyList();

        logger.info("请求返回 Top {} 路径，实际找到 {} 条。", k, topPaths.size());
        if (!topPaths.isEmpty()) {
            logger.info("----- Top {} 路径详情 (F7) -----", Math.min(5, topPaths.size())); // 最多打印前5条
            topPaths.stream()
                    .limit(5) // 最多迭代前5条进行打印
                    .forEach(pf -> {
                        List<PointCoordinate> coords = pf.getPathCoordinates();
                        int coordCount = (coords != null) ? coords.size() : 0;
                        String pathString = "无坐标";
                        if (coordCount > 0) {
                            pathString = coords.stream()
                                    .map(point -> String.format("(%.4f,%.4f)", point.getLongitude(), point.getLatitude()))
                                    .collect(Collectors.joining(" -> "));
                        }
                        logger.info("  路径 ({}个点): {}, 频率: {}",
                                coordCount,
                                pathString,
                                pf.getFrequency()
                        );
                    });
            logger.info("------------------------------");
        } else {
            logger.warn("F7 分析结果中未找到符合条件的频繁路径，或路径列表为空。");
        }
        logMemoryUsage("F7 分析后"); // 记录分析结束后的内存使用情况
        logger.info("=================================");

        // 4. 基本断言
        assertThat(result).as("F7 分析结果不应为 null").isNotNull();
        assertThat(topPaths).as("获取到的路径列表不应为 null").isNotNull();
        assertThat(topPaths.size()).as("返回的路径数量应小于或等于请求的k值").isLessThanOrEqualTo(k);
        // 可以根据预期添加更多断言，例如检查频率是否递减等
    }

    /**
     * !!! 警告: 此测试处理全量数据，可能耗时较长并占用大量内存 !!!
     * !!! 运行前请确认数据路径和 JVM 内存 (-Xmx) 设置是否充足 !!!
     */
    @Test
    @DisplayName("性能测试: F8 - 分析区域间频繁路径 (全量数据)")
    //@Disabled("默认禁用此耗时测试，请手动启用以在全量数据上运行性能评估")
    void performanceTest_F8_FullData() {
        logger.warn("!!! 开始 F8 全量数据性能测试（区域 A -> 区域 B），此过程可能耗时较长并占用大量内存 !!!");

        // 1. 定义查询参数
        int k = 20; // 获取 Top 20 频繁路径
        double minDistanceKM = 0.5; // 路径的最小地理距离为 0.5 千米
        FrequentPathQuery query = new FrequentPathQuery(k, minDistanceKM, regionA, regionB);
        logger.info("测试查询参数 (F8 - 区域 A 到 B): 起始区域 A={}, 目标区域 B={}, {}", regionA, regionB, query);
        logMemoryUsage("F8 分析前");

        // 2. 执行分析并使用 StopWatch 计时
        StopWatch stopWatch = new StopWatch("F8 全量数据性能测试 (区域 A->B)");
        FrequentPathResult result = null;
        try {
            stopWatch.start("F8 分析任务");
            result = frequentPathService.analyzeFrequentPaths(query);
            stopWatch.stop();
        } catch (OutOfMemoryError oom) {
            logger.error("!!! F8 性能测试期间发生内存不足错误 (OutOfMemoryError)！请考虑增加 JVM 堆内存 (-Xmx参数) !!!", oom);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("内存溢出后");
            throw oom;
        } catch (Exception e) {
            logger.error("!!! F8 性能测试期间发生未预期的异常 !!!", e);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("异常发生后");
            throw e;
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
                logger.warn("StopWatch 在 finally 块中被强制停止。");
            }
        }

        // 3. 记录分析结果和性能数据
        logger.info("===== F8 全量数据性能测试结果 (区域 A -> 区域 B) =====");
        logger.info("执行总耗时: {} 毫秒 (约 {} 秒)",
                stopWatch.getTotalTimeMillis(),
                String.format("%.2f", stopWatch.getTotalTimeSeconds()));

        List<PathFrequency> topPaths = (result != null) ? result.getPathFrequencies() : Collections.emptyList();

        logger.info("请求返回 Top {} 路径，实际找到 {} 条。", k, topPaths.size());
        if (!topPaths.isEmpty()) {
            logger.info("----- Top {} 路径详情 (F8 A->B) -----", Math.min(5, topPaths.size()));
            topPaths.stream()
                    .limit(5)
                    .forEach(pf -> {
                        List<PointCoordinate> coords = pf.getPathCoordinates();
                        int coordCount = (coords != null) ? coords.size() : 0;
                        String pathString = "无坐标";
                        if (coordCount > 0) {
                            // 修改点：格式化坐标点输出
                            pathString = coords.stream()
                                    .map(point -> String.format("(%.4f,%.4f)", point.getLongitude(), point.getLatitude()))
                                    .collect(Collectors.joining(" -> "));
                        }
                        logger.info("  路径 ({}个点): {}, 频率: {}",
                                coordCount,
                                pathString,
                                pf.getFrequency()
                        );
                    });
            logger.info("----------------------------------");
        } else {
            logger.warn("F8 分析结果中未找到符合条件的频繁路径，或路径列表为空。");
        }
        logMemoryUsage("F8 分析后");
        logger.info("========================================");

        // 4. 基本断言
        assertThat(result).as("F8 分析结果不应为 null").isNotNull();
        assertThat(topPaths).as("获取到的路径列表不应为 null (F8)").isNotNull();
        assertThat(topPaths.size()).as("返回的路径数量应小于或等于请求的k值 (F8)").isLessThanOrEqualTo(k);
    }

    /**
     * 辅助方法：打印当前JVM内存使用情况。
     * @param context 描述当前记录内存使用情况的上下文信息（例如 "分析前", "分析后"）。
     */
    private void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemoryBytes = runtime.totalMemory(); // JVM已分配的总内存
        long freeMemoryBytes = runtime.freeMemory();   // JVM已分配内存中的空闲部分
        long usedMemoryBytes = totalMemoryBytes - freeMemoryBytes; // JVM当前实际使用的内存
        long maxMemoryBytes = runtime.maxMemory();     // JVM可使用的最大内存（受-Xmx参数限制）

        logger.info("[内存使用情况 - {}]: 已用={} MB, 已分配={} MB, 最大堆内存={} MB",
                context,
                usedMemoryBytes / (1024 * 1024),  // 转换为 MB
                totalMemoryBytes / (1024 * 1024), // 转换为 MB
                maxMemoryBytes == Long.MAX_VALUE ? "无限制" : maxMemoryBytes / (1024 * 1024) // 转换为 MB
        );
    }
}
