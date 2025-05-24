package com.codex.taxitrajectory; // 假设Service在此包下

import com.codex.taxitrajectory.model.core.Region;
// 确保导入重命名和修改后的查询类
import com.codex.taxitrajectory.model.query.CorrelationQuery.RegionCorrelationQuery;
import com.codex.taxitrajectory.model.query.CorrelationQuery.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.model.result.CorrelationResult;
import com.codex.taxitrajectory.repository.TaxiRepository; // 实际注入
import com.codex.taxitrajectory.service.CorrelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // 加载完整应用上下文
class CorrelationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationServiceTest.class);

    @Autowired
    private CorrelationService correlationService; 

    @Autowired
    private TaxiRepository taxiRepository; 

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private Region regionA_f5, regionB_f5, region_f6;
    private LocalDateTime testStartTime, testMidTime, testEndTime;
    private final int timeSlotMinutes = 30;

    @BeforeEach
    void setUp() {
        regionA_f5 = new Region(116.45, 40.00, 116.50, 40.05); 
        regionB_f5 = new Region(116.44, 39.90, 116.48, 39.92);
        
        region_f6 = new Region(116.30, 39.95, 116.35, 40.00); // 例如，中关村软件园附近


        testStartTime = LocalDateTime.of(2008, 2, 2, 14, 0, 0); 
        testMidTime = LocalDateTime.of(2008, 2, 2, 15, 0, 0);   
        testEndTime = LocalDateTime.of(2008, 2, 2, 16, 0, 0);
    }

    @Test
    @DisplayName("F5: 测试两区域间车流量分析（基本执行和结构检查）")
    void testF5_analyzeTrafficFlowChangeBetweenRegions_runsAndReturnsStructure() {
        RegionCorrelationQuery query = new RegionCorrelationQuery();
        query.setStartTime(testStartTime);
        query.setEndTime(testEndTime); 
        query.setTimeSlotMinutes(timeSlotMinutes);
        query.setRegion1(regionA_f5);
        query.setRegion2(regionB_f5);

        logger.info("F5测试: 执行查询 {}", query);
        CorrelationResult result = correlationService.analyzeTrafficFlowChangeBetweenRegions(query);

        assertNotNull(result, "F5分析结果不应为null");
        assertNotNull(result.getTrafficFlowChange(), "F5车流量变化Map不应为null");

        for (Map.Entry<LocalDateTime, int[]> entry : result.getTrafficFlowChange().entrySet()) {
            logger.info("F5结果: 时间槽 [{}], 车流量 [1->2: {}, 2->1: {}]",
                    entry.getKey().format(formatter), entry.getValue()[0], entry.getValue()[1]);
            assertNotNull(entry.getValue(), "每个时间槽的车流量数组不应为null");
            assertTrue(entry.getValue()[0] >= 0, "从区域1到区域2的车流量不应为负");
            assertTrue(entry.getValue()[1] >= 0, "从区域2到区域1的车流量不应为负");
        }
        logger.info("F5测试: 基本执行和结构检查通过。");
    }

    @Test
    @DisplayName("F6: 测试单区域与其他区域车流量分析（基本执行和结构检查）")
    void testF6_analyzeTrafficFlowChangeWithOtherRegions_runsAndReturnsStructure() {
        RegionSingleCorrelationQuery query = new RegionSingleCorrelationQuery();
        query.setStartTime(testStartTime);
        query.setEndTime(testEndTime);
        query.setTimeSlotMinutes(timeSlotMinutes);
        query.setTopLeftLongitude(region_f6.getMinLon());
        query.setTopLeftLatitude(region_f6.getMaxLat());
        query.setBottomRightLongitude(region_f6.getMaxLon());
        query.setBottomRightLatitude(region_f6.getMinLat());

        logger.info("F6测试: 执行查询 {}", query);
        CorrelationResult result = correlationService.analyzeTrafficFlowChangeWithOtherRegions(query);

        assertNotNull(result, "F6分析结果不应为null");
        assertNotNull(result.getTrafficFlowChange(), "F6车流量变化Map不应为null");

        assertTrue(result.getTrafficFlowChange().containsKey(testStartTime), "F6结果应包含第一个时间槽的开始时间");
        assertTrue(result.getTrafficFlowChange().containsKey(testMidTime), "F6结果应包含第二个时间槽的开始时间");

        for (Map.Entry<LocalDateTime, int[]> entry : result.getTrafficFlowChange().entrySet()) {
            logger.info("F6结果: 时间槽 [{}], 车流量 [进入: {}, 离开: {}]",
                    entry.getKey().format(formatter), entry.getValue()[0], entry.getValue()[1]);
            assertNotNull(entry.getValue(), "每个时间槽的车流量数组不应为null");
            assertTrue(entry.getValue()[0] >= 0, "进入区域的车流量不应为负");
            assertTrue(entry.getValue()[1] >= 0, "离开区域的车流量不应为负");
        }
        logger.info("F6测试: 基本执行和结构检查通过。");
    }
}