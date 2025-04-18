package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.service.FrequentPathService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // 允许控制执行顺序
public class FrequentPathServicePerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathServicePerformanceTest.class);

    @Autowired
    private FrequentPathService frequentPathService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 定义不同的测试场景参数
    private static final FrequentPathQuery QUERY_SHORT_TERM = new FrequentPathQuery();
    private static final FrequentPathQuery QUERY_MEDIUM_TERM = new FrequentPathQuery();
    private static final FrequentPathQuery QUERY_LONG_TERM = new FrequentPathQuery();

    @BeforeAll
    static void setupQueries() {
        // 短时间范围 (例如 6 小时)
        QUERY_SHORT_TERM.setK(10);
        QUERY_SHORT_TERM.setMinDistance(1.0); // 1 公里
        QUERY_SHORT_TERM.setStartTime(LocalDateTime.parse("2008-02-02 12:00:00", formatter));
        QUERY_SHORT_TERM.setEndTime(LocalDateTime.parse("2008-02-02 18:00:00", formatter));

        // 中等时间范围 (例如 1 天)
        QUERY_MEDIUM_TERM.setK(10);
        QUERY_MEDIUM_TERM.setMinDistance(1.0);
        QUERY_MEDIUM_TERM.setStartTime(LocalDateTime.parse("2008-02-03 00:00:00", formatter));
        QUERY_MEDIUM_TERM.setEndTime(LocalDateTime.parse("2008-02-04 00:00:00", formatter));

        // 较长时间范围 (例如 3 天)
        QUERY_LONG_TERM.setK(10);
        QUERY_LONG_TERM.setMinDistance(1.0);
        QUERY_LONG_TERM.setStartTime(LocalDateTime.parse("2008-02-02 00:00:00", formatter));
        QUERY_LONG_TERM.setEndTime(LocalDateTime.parse("2008-02-05 00:00:00", formatter));
    }

    // Helper method to run and time the service call
    private void runAndLogPerformance(String testName, FrequentPathQuery query) {
        logger.info("========== Starting Performance Test: {} ==========", testName);
        logger.info("Query Parameters: k={}, minDistance={}, startTime={}, endTime={}",
                query.getK(), query.getMinDistance(), query.getStartTime(), query.getEndTime());

        long startTimeNanos = System.nanoTime();
        FrequentPathResult result = null;
        try {
            result = frequentPathService.findFrequentPaths(query);
        } catch (Exception e) {
            logger.error("Exception during performance test '{}': {}", testName, e.getMessage(), e);
            fail("Test " + testName + " failed with exception.", e);
        }
        long endTimeNanos = System.nanoTime();
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTimeNanos - startTimeNanos);

        logger.info("========== Finished Performance Test: {} ==========", testName);
        logger.info("Total Execution Time: {} ms", durationMillis);
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getPathFrequencies(), "Path frequencies list should not be null");
        logger.info("Found {} frequent paths.The top one is: {}", result.getPathFrequencies().size(),result.getPathFrequencies().getFirst());
        // 你可以根据需要添加更具体的断言，比如检查返回数量是否 <= k

        // 这里可以设置一个大致的性能目标（非常依赖环境，谨慎使用）
        // assertTrue(durationMillis < 60000, "Execution time exceeded 60 seconds for " + testName);
    }

    @Test
    @Order(1)
    @DisplayName("性能测试 - 短时间范围 (6小时)")
    void testPerformanceShortTerm() {
        runAndLogPerformance("Short Term (6h)", QUERY_SHORT_TERM);
    }

    @Test
    @Order(2)
    @DisplayName("性能测试 - 中等时间范围 (1天)")
    void testPerformanceMediumTerm() {
        runAndLogPerformance("Medium Term (1d)", QUERY_MEDIUM_TERM);
    }

    @Test
    @Order(3)
    @DisplayName("性能测试 - 长时间范围 (3天)")
    //@Disabled("Long term test might be very slow, enable manually if needed") // 可以设置禁用，避免CI超时 （实际上不会）
    void testPerformanceLongTerm() {
        runAndLogPerformance("Long Term (3d)", QUERY_LONG_TERM);
    }

    @AfterAll
    static void tearDownAll() {
        logger.info("FrequentPathService performance tests finished.");
    }
}