package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.query.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.service.RegionSingleCorrelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegionSingleCorrelationServiceTest {

    @Autowired
    private RegionSingleCorrelationService regionSingleCorrelationService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    public void setUp() {
        // 可以在这里进行一些初始化操作，如果需要的话
    }

    /**
     * 测试基本的区域关联分析2功能
     */
    @Test
    public void testAnalyzeTrafficFlowChangeWithOtherRegions() {
        // 创建区域关联分析2查询参数
        RegionSingleCorrelationQuery query = new RegionSingleCorrelationQuery();
        query.setStartTime(LocalDateTime.parse("2008-02-02 08:00:00", formatter));
        query.setEndTime(LocalDateTime.parse("2008-02-03 10:00:00", formatter));
        query.setTimeSlotMinutes(30);

        // 设置指定矩形区域的坐标
        query.setTopLeftLongitude(116);
        query.setTopLeftLatitude(39.5);
        query.setBottomRightLongitude(116.5);
        query.setBottomRightLatitude(40.0);

        // 调用服务方法
        Map<LocalDateTime, int[]> result = regionSingleCorrelationService.analyzeTrafficFlowChangeWithOtherRegions(query);

        // 验证结果
        assertNotNull(result, "分析结果不应为空");
        assertFalse(result.isEmpty(), "结果应该包含至少一个时间槽的数据");

        // 打印每个时间槽的统计信息
        for (Map.Entry<LocalDateTime, int[]> entry : result.entrySet()) {
            LocalDateTime timeSlot = entry.getKey();
            int[] flow = entry.getValue();

            System.out.println("时间槽: " + timeSlot);
            System.out.println("  进入指定区域的车流量: " + flow[0]);
            System.out.println("  离开指定区域的车流量: " + flow[1]);
        }
    }
}