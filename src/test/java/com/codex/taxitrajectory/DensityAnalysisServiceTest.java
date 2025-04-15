package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F4 区域车流密度分析功能测试
 */
@SpringBootTest
public class DensityAnalysisServiceTest {

    @Autowired
    private DensityAnalysisService densityAnalysisService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testBasicDensityAnalysis() {
        DensityQuery query = new DensityQuery();
        query.setGridSize(1.0); // 1公里网格
        query.setStartTime(LocalDateTime.parse("2008-02-02 15:00:00", formatter));
        query.setEndTime(LocalDateTime.parse("2008-02-02 16:00:00", formatter));
        query.setTimeSlotMinutes(60); // 1小时时间槽
        // 可选区域参数，默认北京市区范围
        query.setMinLongitude(116.0);
        query.setMinLatitude(39.6);
        query.setMaxLongitude(117.0);
        query.setMaxLatitude(40.2);

        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        assertNotNull(result, "分析结果不应为空");
        assertNotNull(result.getGrid(), "网格信息不应为空");
        assertNotNull(result.getTimeSlots(), "时间槽列表不应为空");
        assertNotNull(result.getDensityMap(), "密度映射不应为空");

        // 验证时间槽数量
        assertEquals(2, result.getTimeSlots().size(), "应只有两个时间槽");

        // 验证网格数量合理
        int totalCells = result.getGrid().getRows() * result.getGrid().getCols();
        assertTrue(totalCells > 0, "网格单元数量应大于0");

        // 验证密度数据存在
        Map<String, Integer> densityAtTime = result.getDensityMap().get(result.getTimeSlots().get(0));
        assertNotNull(densityAtTime, "时间槽对应的密度数据不应为空");
        assertFalse(densityAtTime.isEmpty(), "密度数据不应为空");

        // 至少有一个网格密度大于0
        boolean hasTraffic = densityAtTime.values().stream().anyMatch(d -> d > 0);
        assertTrue(hasTraffic, "应至少有一个网格的车流密度大于0");

        System.out.println("=== 测试结果预览 ===");
        System.out.println("查询时间范围：" + query.getStartTime() + " ~ " + query.getEndTime());
        System.out.println("网格大小：" + query.getGridSize() + " km");
        System.out.printf("经纬度范围：%.3f ~ %.3f 经度，%.3f ~ %.3f 纬度%n",
                query.getMinLongitude(), query.getMaxLongitude(),
                query.getMinLatitude(), query.getMaxLatitude());
        System.out.printf("网格行列数：%d x %d，总计：%d 个网格%n",
                result.getGrid().getRows(), result.getGrid().getCols(), totalCells);

        for (LocalDateTime slot : result.getTimeSlots()) {
            Map<String, Integer> slotDensity = result.getDensityMap().get(slot);
            long nonEmpty = slotDensity.values().stream().filter(d -> d > 0).count();
            int maxDensity = slotDensity.values().stream().max(Integer::compareTo).orElse(0);
            System.out.printf("时间槽：%s | 非空网格数：%d | 最大密度：%d%n",
                    slot.toString(), nonEmpty, maxDensity);
        }

        // 可选：输出前几个网格的密度情况
        System.out.println("示例网格密度值（部分）：");
        result.getDensityMap().get(result.getTimeSlots().get(0))
                .entrySet().stream().limit(10).forEach(entry ->
                        System.out.println("网格 " + entry.getKey() + " -> 密度：" + entry.getValue()));
    }

    @Test
    public void testMultipleTimeSlotsDensityAnalysis() {
        DensityQuery query = new DensityQuery();
        query.setGridSize(1.0); // 1公里网格
        query.setStartTime(LocalDateTime.parse("2008-02-02 14:30:00", formatter));
        query.setEndTime(LocalDateTime.parse("2008-02-02 15:30:00", formatter));
        query.setTimeSlotMinutes(30); // 30分钟时间槽
        query.setMinLongitude(116.0);
        query.setMinLatitude(39.6);
        query.setMaxLongitude(117.0);
        query.setMaxLatitude(40.2);

        // 执行车流密度分析
        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        // 验证结果不为空
        assertNotNull(result, "分析结果不应为空");
        assertNotNull(result.getGrid(), "网格信息不应为空");
        assertNotNull(result.getTimeSlots(), "时间槽列表不应为空");
        assertNotNull(result.getDensityMap(), "密度映射不应为空");

        // 验证时间槽数量正确（每30分钟为一个时间槽，期望3个时间槽）
        assertEquals(3, result.getTimeSlots().size(), "应有3个时间槽");

        // 验证网格数量合理
        int totalCells = result.getGrid().getRows() * result.getGrid().getCols();
        assertTrue(totalCells > 0, "网格单元数量应大于0");

        // 验证每个时间槽的密度数据
        for (LocalDateTime slot : result.getTimeSlots()) {
            Map<String, Integer> slotDensity = result.getDensityMap().get(slot);
            assertNotNull(slotDensity, "时间槽对应的密度数据不应为空");
            assertFalse(slotDensity.isEmpty(), "密度数据不应为空");

            long nonEmptyCells = slotDensity.values().stream().filter(d -> d > 0).count();
            assertTrue(nonEmptyCells > 0, "每个时间槽至少应有一个网格的车流密度大于0");

            // 输出每个时间槽的网格数据
            int maxDensity = slotDensity.values().stream().max(Integer::compareTo).orElse(0);
            System.out.printf("时间槽：%s | 非空网格数：%d | 最大密度：%d%n",
                    slot.toString(), nonEmptyCells, maxDensity);
        }
//
//        // 可选：输出前几个网格的密度情况
//        System.out.println("示例网格密度值（部分）：");
//        result.getDensityMap().get(result.getTimeSlots().get(0))
//                .entrySet().stream().limit(10).forEach(entry ->
//                        System.out.println("网格 " + entry.getKey() + " -> 密度：" + entry.getValue()));
    }
}
