package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.query.RegionCorrelationQuery;
import com.codex.taxitrajectory.service.RegionCorrelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegionCorrelationServiceTest {

    @Autowired
    private RegionCorrelationService regionCorrelationService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    public void setUp() {
        // 可以在这里进行一些初始化操作，如果需要的话
    }

    /**
     * 测试基本的区域关联分析功能
     */
    @Test
    public void testAnalyzeTrafficFlowChangeBetweenRegions() {
        // 创建区域关联分析查询参数
        RegionCorrelationQuery query = new RegionCorrelationQuery();
        query.setStartTime(LocalDateTime.parse("2008-02-02 08:00:00", formatter));
        query.setEndTime(LocalDateTime.parse("2008-02-03 10:00:00", formatter));
        query.setTimeSlotMinutes(30);

        // 设置第一个区域的坐标
        query.setTopLeftLongitude1(116);
        query.setTopLeftLatitude1(39.5);
        query.setBottomRightLongitude1(116.5);
        query.setBottomRightLatitude1(40.0);

        // 设置第二个区域的坐标
        query.setTopLeftLongitude2(116.5);
        query.setTopLeftLatitude2(40);
        query.setBottomRightLongitude2(117);
        query.setBottomRightLatitude2(40.5);

        // 调用服务方法
        Map<LocalDateTime, int[]> result = regionCorrelationService.analyzeTrafficFlowChangeBetweenRegions(query);

        // 验证结果
        assertNotNull(result, "分析结果不应为空");
        assertFalse(result.isEmpty(), "结果应该包含至少一个时间槽的数据");

        // 打印每个时间槽的统计信息
        for (Map.Entry<LocalDateTime, int[]> entry : result.entrySet()) {
            LocalDateTime timeSlot = entry.getKey();
            int[] flow = entry.getValue();

            System.out.println("时间槽: " + timeSlot);
            System.out.println("  从区域1到区域2的车流量: " + flow[0]);
            System.out.println("  从区域2到区域1的车流量: " + flow[1]);
        }
    }
}