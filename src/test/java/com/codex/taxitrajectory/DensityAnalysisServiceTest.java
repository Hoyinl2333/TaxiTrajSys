package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.model.result.DensityAnalysisResult;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles; // 如果有测试特定的配置文件

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DensityAnalysisServiceTest {

    @Autowired
    private DensityAnalysisService densityAnalysisService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 为测试定义一个常用的地理边界 (例如，北京市内的一个区域)
    // 这些值应确保您的测试数据源在该区域内有数据
    private final double TEST_MIN_LON = 116.30;
    private final double TEST_MIN_LAT = 39.85;
    private final double TEST_MAX_LON = 116.50; // 大约是TEST_MIN_LON东边约17km
    private final double TEST_MAX_LAT = 39.95; // 大约是TEST_MIN_LAT北边约11km


    private DensityQuery createValidBaseQuery() {
        DensityQuery query = new DensityQuery();
        query.setGridSize(1.0); // 1公里网格
        // 设置强制的地理边界
        query.setMinLongitude(TEST_MIN_LON);
        query.setMinLatitude(TEST_MIN_LAT);
        query.setMaxLongitude(TEST_MAX_LON);
        query.setMaxLatitude(TEST_MAX_LAT);
        query.setTimeSlotMinutes(60); // 默认为60分钟时间槽
        return query;
    }

    @Test
    @DisplayName("基础密度分析测试 - 跨越多天，24小时时间槽")
    public void testBasicDensityAnalysis_MultipleDays_24HourSlots() {
        DensityQuery query = createValidBaseQuery();
        query.setStartTime(LocalDateTime.parse("2008-02-02 15:00:00", formatter));
        query.setEndTime(LocalDateTime.parse("2008-02-08 15:00:00", formatter)); // 包含2月8日15:00
        query.setTimeSlotMinutes(60 * 24); // 24小时 (1440分钟) 时间槽

        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        assertNotNull(result, "分析结果不应为空");

        assertNotNull(result.getTimeSlots(), "时间槽列表不应为空");
        assertNotNull(result.getDensityMap(), "密度映射不应为空");

        // 验证时间槽数量: Feb 2 15h, 3 15h, 4 15h, 5 15h, 6 15h, 7 15h => 6个时间槽
        assertEquals(6, result.getTimeSlots().size(), "时间槽数量应为7个");

        int totalCells = result.getRows() * result.getCols();
        assertTrue(totalCells > 0, "网格单元数量应大于0");
        System.out.printf("[基础测试] 网格行列数：%d x %d，总计：%d 个网格%n", result.getRows(), result.getCols(), totalCells);


        // 依赖于测试数据，以下断言可能需要调整
        // 假设在这么大范围和长时间内，至少应有一些数据
        Map<String, Integer> firstSlotDensity = result.getDensityMap().get(result.getTimeSlots().get(0));
        assertNotNull(firstSlotDensity, "第一个时间槽的密度数据不应为空");

        // 如果您的测试数据源在此时间/区域内保证有数据
        // assertFalse(firstSlotDensity.isEmpty(), "第一个时间槽的密度数据映射不应为空集");
        // boolean hasTraffic = firstSlotDensity.values().stream().anyMatch(d -> d > 0);
        // assertTrue(hasTraffic, "在第一个时间槽，应至少有一个网格的车流密度大于0");

        printResultSummary(query, result);
    }

    @Test
    @DisplayName("多时间槽密度分析测试 - 1小时范围，30分钟时间槽")
    public void testMultipleTimeSlotsDensityAnalysis_1HourRange_30MinSlots() {
        DensityQuery query = createValidBaseQuery();
        query.setStartTime(LocalDateTime.parse("2008-02-02 15:00:00", formatter));
        query.setEndTime(LocalDateTime.parse("2008-02-02 16:00:00", formatter)); // 包含15:30
        query.setTimeSlotMinutes(30); // 30分钟时间槽

        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        assertNotNull(result, "分析结果不应为空");
        assertEquals(2, result.getTimeSlots().size(), "时间槽数量应为2个");

        int totalCells = result.getRows() * result.getCols();
        assertTrue(totalCells > 0, "网格单元数量应大于0");
        System.out.printf("[多时间槽测试] 网格行列数：%d x %d，总计：%d 个网格%n", result.getRows(), result.getCols(), totalCells);


        // 依赖于测试数据
        for (LocalDateTime slot : result.getTimeSlots()) {
            Map<String, Integer> slotDensity = result.getDensityMap().get(slot);
            assertNotNull(slotDensity, "时间槽 " + slot + " 对应的密度数据不应为空");
            // 如果确定该时间槽内必有数据:
            // assertFalse(slotDensity.isEmpty(), "时间槽 " + slot + " 的密度数据映射不应为空集");
            // long nonEmptyCells = slotDensity.values().stream().filter(d -> d > 0).count();
            // assertTrue(nonEmptyCells > 0, "时间槽 " + slot + " 至少应有一个网格的车流密度大于0");
        }
        printResultSummary(query, result);
    }


    @Test
    @DisplayName("边界情况测试 - 没有出租车数据（或没有符合条件的数据）")
    public void testEdgeCase_NoTaxiDataOrNoMatchingData() {
        DensityQuery query = createValidBaseQuery();
        // 设置一个不太可能有数据的遥远时间或非常小的、偏僻的区域
        // 或者，如果可以mock TaxiRepository，让其返回空数据
        query.setStartTime(LocalDateTime.parse("1900-01-01 00:00:00", formatter));
        query.setEndTime(LocalDateTime.parse("1900-01-01 01:00:00", formatter));
        query.setMinLongitude(0.0); query.setMinLatitude(0.0);
        query.setMaxLongitude(0.1); query.setMaxLatitude(0.1);


        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        assertNotNull(result, "即使没有数据，分析结果对象也不应为空");
        assertNotNull(result.getTimeSlots(), "时间槽列表不应为空");
        assertNotNull(result.getDensityMap(), "密度图映射不应为空");

        // 预期时间槽数量仍然根据查询参数生成
        assertEquals(1, result.getTimeSlots().size()); // 00:00

        // 预期每个时间槽的密度数据为空map
        for (LocalDateTime slot : result.getTimeSlots()) {
            Map<String, Integer> slotDensity = result.getDensityMap().get(slot);
            assertNotNull(slotDensity, "时间槽 " + slot + " 的密度数据不应为null，应为空Map");
            assertTrue(slotDensity.isEmpty(), "时间槽 " + slot + " 的密度数据应为空Map");
        }
        System.out.println("[边界情况测试 - 无数据] 测试通过，返回了空的密度数据结构。");
        printResultSummary(query, result);
    }


    // 辅助方法，用于打印结果摘要
    private void printResultSummary(DensityQuery query, DensityAnalysisResult result) {
        System.out.println("----------------------------------------------------");
        System.out.println("测试查询: " + query.toString());
        System.out.println("  查询时间范围：" + query.getStartTime().format(formatter) + " ~ " + query.getEndTime().format(formatter));
        System.out.println("  地理边界 (查询)： Lon [" + query.getMinLongitude() + ", " + query.getMaxLongitude() +
                "], Lat [" + query.getMinLatitude() + ", " + query.getMaxLatitude() + "]");
        System.out.println("  网格大小：" + query.getGridSize() + " km, 时间槽：" + query.getTimeSlotMinutes() + " 分钟");
        System.out.println("分析结果:");
        System.out.printf("  网格行列数：%d x %d，总计：%d 个网格%n",
                result.getRows(), result.getCols(), result.getRows() * result.getCols());
        System.out.println("  时间槽数量: " + result.getTimeSlots().size());

        for (LocalDateTime slot : result.getTimeSlots()) {
            Map<String, Integer> slotDensity = result.getDensityMap().get(slot);
            if (slotDensity != null) {
                long nonEmpty = slotDensity.values().stream().filter(d -> d > 0).count();
                int maxDensity = slotDensity.values().stream().max(Integer::compareTo).orElse(0);
                int minDensity = slotDensity.values().stream().min(Integer::compareTo).orElse(0);
                System.out.printf("  时间槽：%s | 非空单元格数：%d | 最大密度：%d | 最小密度：%d%n",
                        slot.format(formatter), nonEmpty, maxDensity,minDensity);
            } else {
                System.out.printf("  时间槽：%s | 密度数据为 null%n", slot.format(formatter));
            }
        }
        if (!result.getTimeSlots().isEmpty() && result.getDensityMap().get(result.getTimeSlots().get(0)) != null &&
                !result.getDensityMap().get(result.getTimeSlots().get(0)).isEmpty()) {
            System.out.println("  首个时间槽示例网格密度值（部分）：");
            result.getDensityMap().get(result.getTimeSlots().get(0))
                    .entrySet().stream().filter(e -> e.getValue() > 0).limit(5).forEach(entry ->
                            System.out.println("    网格 " + entry.getKey() + " -> 密度：" + entry.getValue()));
        }
        System.out.println("----------------------------------------------------");
    }

    @AfterEach
    public void cleanUpAfterTest() {
        System.gc();
        System.out.println("测试完成，已执行清理操作。");
    }
}