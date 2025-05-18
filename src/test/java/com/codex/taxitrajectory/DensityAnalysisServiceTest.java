package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.DensityQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DensityAnalysisServiceTest {

    @Autowired
    private DensityAnalysisService densityAnalysisService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试基本车流密度分析功能
     */
    @Test
    public void testAnalyzeTrafficDensity() {
        // 创建密度分析查询参数
        DensityQuery query = new DensityQuery(
                1.0, // 网格大小为1公里
                LocalDateTime.parse("2008-02-02 15:00:00", formatter),
                LocalDateTime.parse("2008-02-02 16:00:00", formatter),
                60   // 时间粒度为60分钟
        );

        // 可选：设置区域边界
        query.setMinLongitude(116.3);
        query.setMinLatitude(39.8);
        query.setMaxLongitude(116.5);
        query.setMaxLatitude(40.0);

        // 调用服务方法
        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        // 验证结果
        assertNotNull(result, "分析结果不应为空");
        assertNotNull(result.getGrid(), "网格信息不应为空");
        assertNotNull(result.getTimeSlots(), "时间槽列表不应为空");
        assertNotNull(result.getDensityMap(), "密度映射不应为空");

        // 验证时间槽数量
        assertEquals(1, result.getTimeSlots().size(), "应该只有一个时间槽");

        // 验证网格数量
        int totalGridCells = result.getGrid().getRows() * result.getGrid().getCols();
        assertTrue(totalGridCells > 0, "网格单元数量应大于0");

        // 打印结果摘要
        System.out.println("网格大小: " + query.getGridSize() + "公里");
        System.out.println("网格行数: " + result.getGrid().getRows());
        System.out.println("网格列数: " + result.getGrid().getCols());
        System.out.println("总网格数: " + totalGridCells);

        // 验证是否有车流密度数据
        Map<LocalDateTime, Map<String, Integer>> densityMap = result.getDensityMap();
        LocalDateTime timeSlot = result.getTimeSlots().get(0);
        Map<String, Integer> cellDensities = densityMap.get(timeSlot);

        assertNotNull(cellDensities, "单元格密度数据不应为空");
        assertFalse(cellDensities.isEmpty(), "应该有密度数据");

        // 验证是否有密度大于0的网格
        boolean hasTraffic = cellDensities.values().stream().anyMatch(density -> density > 0);
        assertTrue(hasTraffic, "应该至少有一个网格的密度大于0");
    }

    /**
     * 测试多时间段车流密度分析
     */
    @Test
    public void testMultipleTimeSlots() {
        // 创建密度分析查询参数 - 更长时间段和更小时间粒度
        DensityQuery query = new DensityQuery(
                0.5, // 网格大小为0.5公里
                LocalDateTime.parse("2008-02-02 08:00:00", formatter),
                LocalDateTime.parse("2008-02-02 10:00:00", formatter),
                30   // 时间粒度为30分钟
        );

        // 设置区域边界（与之前测试相同）
        query.setMinLongitude(116.3);
        query.setMinLatitude(39.8);
        query.setMaxLongitude(116.5);
        query.setMaxLatitude(40.0);

        // 调用服务方法
        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        // 验证结果
        assertNotNull(result, "分析结果不应为空");

        // 验证时间槽数量
        assertEquals(4, result.getTimeSlots().size(), "应该有4个时间槽");

        // 打印每个时间槽的统计信息
        Map<LocalDateTime, Map<String, Integer>> densityMap = result.getDensityMap();

        for (LocalDateTime timeSlot : result.getTimeSlots()) {
            Map<String, Integer> cellDensities = densityMap.get(timeSlot);
            assertNotNull(cellDensities, timeSlot + " 的密度数据不应为空");

            // 计算该时间段的总车辆数和有车网格数
            int totalTaxis = cellDensities.values().stream().mapToInt(Integer::intValue).sum();
            long nonEmptyGrids = cellDensities.values().stream().filter(v -> v > 0).count();

            System.out.println("时间槽: " + timeSlot);
            System.out.println("  总车辆数: " + totalTaxis);
            System.out.println("  有车网格数: " + nonEmptyGrids);

            // 获取最高密度
            int maxDensity = cellDensities.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
            System.out.println("  最高密度: " + maxDensity);
        }
    }

    /**
     * 测试早高峰时段分析
     */
    @Test
    public void testRushHourAnalysis() {
        // 创建密度分析查询参数 - 早高峰时段
        DensityQuery query = new DensityQuery(
                1.0, // 网格大小为1公里
                LocalDateTime.parse("2008-02-02 07:00:00", formatter),
                LocalDateTime.parse("2008-02-02 09:00:00", formatter),
                30   // 时间粒度为30分钟
        );

        // 调用服务方法
        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        // 验证结果
        assertNotNull(result, "分析结果不应为空");
        assertEquals(4, result.getTimeSlots().size(), "应该有4个时间槽");

        // 打印早高峰时段的密度变化
        System.out.println("\n早高峰时段车流密度分析:");

        Map<LocalDateTime, Map<String, Integer>> densityMap = result.getDensityMap();
        for (LocalDateTime timeSlot : result.getTimeSlots()) {
            Map<String, Integer> cellDensities = densityMap.get(timeSlot);

            // 计算总车辆数
            int totalTaxis = cellDensities.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            // 计算平均密度（只考虑有车的网格）
            double avgDensity = cellDensities.values().stream()
                    .filter(v -> v > 0)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);

            System.out.println("时间: " + timeSlot);
            System.out.println("  总车辆数: " + totalTaxis);
            System.out.println("  平均密度: " + String.format("%.2f", avgDensity));
        }
    }
}
