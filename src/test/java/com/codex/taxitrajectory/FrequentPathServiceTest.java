package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.Region;

import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.service.FrequentPathService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FrequentPathServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathServiceTest.class);

    @Autowired
    private FrequentPathService frequentPathService;

    // 定义 F8 测试区域 (请根据你的数据调整)
    private Region regionA = new Region(116.30, 39.90, 116.35, 39.95); // 示例区域 A
    private Region regionB = new Region(116.40, 39.95, 116.45, 40.00); // 示例区域 B

    /**
     * !!! 警告: 此测试处理全量数据，耗时较长且内存消耗大 !!!
     * !!! 运行前请确认数据路径和 JVM 内存 (-Xmx) !!!
     */
    @Test
    @DisplayName("性能测试: F7 - 分析全市频繁路径 (全量数据)")
    //@Disabled("默认禁用，手动启用以运行全量数据性能测试") // 取消注释以启用
    void performanceTest_F7_FullData() {
        logger.warn("!!! 开始 F7 全量数据性能测试，可能耗时较长并占用大量内存 !!!");

        // 1. 定义查询参数
        int k = 50; // 获取 Top 50
        double minDistanceMeters = 1000; // 至少 1 公里
        FrequentPathQuery query = new FrequentPathQuery(k, minDistanceMeters);
        logger.info("测试查询参数 (F7): {}", query);
        logMemoryUsage("F7 分析前"); // 记录初始内存

        // 2. 执行并计时
        StopWatch stopWatch = new StopWatch("F7 全量数据性能测试");
        FrequentPathResult result = null;
        try {
            stopWatch.start("F7 分析");
            result = frequentPathService.analyzeFrequentPaths(query);
            stopWatch.stop();
        } catch (OutOfMemoryError oom) {
            logger.error("!!! F7 性能测试期间发生内存不足错误 (OutOfMemoryError)！请增加 JVM 堆内存 (-Xmx) !!!", oom);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("内存溢出后");
            throw oom;
        } catch (Exception e) {
            logger.error("!!! F7 性能测试期间发生异常 !!!", e);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("异常发生后");
            throw e;
        } finally {
            if (stopWatch.isRunning()) stopWatch.stop();
        }

        // 3. 记录结果和性能
        logger.info("===== F7 全量数据性能测试结果 =====");
        logger.info("执行耗时: {} 毫秒 ({} 秒)",
                stopWatch.getTotalTimeMillis(), stopWatch.getTotalTimeSeconds());
        if (result != null && result.getPathFrequencies() != null) {
            logger.info("找到 Top {} 频繁路径数量: {}", k, result.getPathFrequencies().size());
            // **** 修改点 1: 打印 Top 5 详细路径 ****
            if (!result.getPathFrequencies().isEmpty()) {
                logger.info("----- Top 5 路径详情 (F7) -----");
                result.getPathFrequencies().stream()
                        .limit(5) // 取前 5 条
                        .forEach(pf -> logger.info("  路径 ({} 个格子): {}, 频率: {}",
                                pf.getPath().getCellIdSequence().size(), // 显示格子数量
                                String.join(" -> ", pf.getPath().getCellIdSequence()), // 用箭头连接格子
                                pf.getFrequency()));
                logger.info("------------------------------");
            }
        } else {
            logger.warn("分析结果或路径列表为空。");
        }
        logMemoryUsage("F7 分析后"); // 记录结束时内存
        logger.info("=================================");

        // 4. 基本断言
        assertThat(result).isNotNull();
        assertThat(result.getPathFrequencies()).isNotNull();
        assertThat(result.getPathFrequencies().size()).isLessThanOrEqualTo(k);
    }

    /**
     * !!! 警告: 此测试处理全量数据，耗时较长且内存消耗大 !!!
     * !!! 运行前请确认数据路径和 JVM 内存 (-Xmx) !!!
     */
    @Test
    @DisplayName("性能测试: F8 - 分析区域间频繁路径 (全量数据)")
    //@Disabled("默认禁用，手动启用以运行全量数据性能测试") // 取消注释以启用
    void performanceTest_F8_FullData() {
        logger.warn("!!! 开始 F8 全量数据性能测试，可能耗时较长并占用大量内存 !!!");

        // 1. 定义查询参数
        int k = 20;
        double minDistanceMeters = 500;
        FrequentPathQuery query = new FrequentPathQuery(k, minDistanceMeters, regionA, regionB);
        logger.info("测试查询参数 (F8): {}", query);
        logMemoryUsage("F8 分析前");

        // 2. 执行并计时
        StopWatch stopWatch = new StopWatch("F8 全量数据性能测试 (A->B)");
        FrequentPathResult result = null;
        try {
            stopWatch.start("F8 分析");
            result = frequentPathService.analyzeFrequentPaths(query);
            stopWatch.stop();
        } catch (OutOfMemoryError oom) {
            logger.error("!!! F8 性能测试期间发生内存不足错误 (OutOfMemoryError)！请增加 JVM 堆内存 (-Xmx) !!!", oom);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("内存溢出后");
            throw oom;
        } catch (Exception e) {
            logger.error("!!! F8 性能测试期间发生异常 !!!", e);
            if (stopWatch.isRunning()) stopWatch.stop();
            logMemoryUsage("异常发生后");
            throw e;
        } finally {
            if (stopWatch.isRunning()) stopWatch.stop();
        }

        // 3. 记录结果和性能
        logger.info("===== F8 全量数据性能测试结果 (区域 A->B) =====");
        logger.info("执行耗时: {} 毫秒 ({} 秒)",
                stopWatch.getTotalTimeMillis(), stopWatch.getTotalTimeSeconds());
        if (result != null && result.getPathFrequencies() != null) {
            logger.info("找到 Top {} 频繁路径数量 (A->B): {}", k, result.getPathFrequencies().size());
            // **** 修改点 2: 打印 Top 5 详细路径 ****
            if (!result.getPathFrequencies().isEmpty()) {
                logger.info("----- Top 5 路径详情 (F8 A->B) -----");
                result.getPathFrequencies().stream()
                        .limit(5)
                        .forEach(pf -> logger.info("  路径 ({} 个格子): {}, 频率: {}",
                                pf.getPath().getCellIdSequence().size(),
                                String.join(" -> ", pf.getPath().getCellIdSequence()),
                                pf.getFrequency()));
                logger.info("----------------------------------");
            }
        } else {
            logger.warn("分析结果或路径列表为空。");
        }
        logMemoryUsage("F8 分析后");
        logger.info("========================================");

        // 4. 基本断言
        assertThat(result).isNotNull();
        assertThat(result.getPathFrequencies()).isNotNull();
        assertThat(result.getPathFrequencies().size()).isLessThanOrEqualTo(k);
    }

    // 辅助方法：打印当前内存使用情况 (中文)
    private void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        long maxBytes = runtime.maxMemory();
        // **** 修改点 3: 修改日志为中文 ****
        logger.info("[内存使用 - {}]: 已用={} MB, 已分配={} MB, 最大堆内存={} MB",
                context,
                usedBytes / (1024 * 1024),
                runtime.totalMemory() / (1024 * 1024),
                maxBytes == Long.MAX_VALUE ? "无限制" : maxBytes / (1024 * 1024));
    }
}