package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.result.RegionQueryResult;
import com.codex.taxitrajectory.service.RegionQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegionQueryServiceTest {

    @Autowired
    private RegionQueryService regionQueryService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试基本区域查询功能
     * 使用已知有数据的坐标区域（实际数据中出租车ID=1出现的位置）
     */
    @Test
    public void testCountTaxisInRegion() {
        // 创建区域查询参数
        RegionQuery query = new RegionQuery(
                116.5, // minLongitude
                39.9,  // minLatitude
                116.52, // maxLongitude
                39.93,  // maxLatitude
                LocalDateTime.parse("2008-02-02 15:30:00", formatter),
                LocalDateTime.parse("2008-02-08 15:40:00", formatter)
        );

        // 调用服务方法
        int taxiCount = regionQueryService.getTaxisInRegion(query).getTaxiCount();

        // 验证结果
        assertTrue(taxiCount > 0, "区域内应该有出租车");
        System.out.println("测试区域内出租车数量: " + taxiCount);
    }

    /**
     * 测试空区域（无出租车）的情况
     */
    @Test
    public void testEmptyRegion() {
        // 创建一个远离北京的区域查询
        RegionQuery query = new RegionQuery(
                110.0, // minLongitude
                30.0,  // minLatitude
                110.1, // maxLongitude
                30.1,  // maxLatitude
                LocalDateTime.parse("2008-02-02 15:30:00", formatter),
                LocalDateTime.parse("2008-02-02 15:40:00", formatter)
        );

        // 调用服务方法
        int taxiCount = regionQueryService.getTaxisInRegion(query).getTaxiCount();

        // 验证结果
        assertEquals(0, taxiCount, "区域内不应该有出租车");
    }

    /**
     * 测试大区域查询（覆盖整个北京市区）
     */
    @Test
    public void testLargeRegion() {
        // 创建覆盖整个北京市区的查询
        RegionQuery query = new RegionQuery(
                116.2, // minLongitude
                39.7,  // minLatitude
                116.6, // maxLongitude
                40.1,  // maxLatitude
                LocalDateTime.parse("2008-02-02 00:00:00", formatter),
                LocalDateTime.parse("2008-02-02 23:59:59", formatter)
        );

        // 调用服务方法
        int taxiCount = regionQueryService.getTaxisInRegion(query).getTaxiCount();


        // 验证结果
        assertTrue(taxiCount > 100, "大区域内应该有大量出租车");
        System.out.println("北京市区一天内出租车数量: " + taxiCount);

    }


    @Test
    public void testLargeRegionLongDuration () {
        // 创建查询条件，范围和时间根据实际需求调整
        RegionQuery query = new RegionQuery(
                116.3, 39.8,  // minLongitude, minLatitude
                116.5, 40.0,  // maxLongitude, maxLatitude
                LocalDateTime.parse("2008-02-03 11:25:00",formatter),
                LocalDateTime.parse("2008-02-05 11:25:00",formatter)
        );

        // 预热阶段：提前调用几次以排除 JVM 预热或缓存加载的影响
        for (int i = 0; i < 2; i++) {
            regionQueryService.getTaxisInRegion(query);
        }

        // 正式测试：执行指定次数，记录总耗时
        final int iterations = 10;
        long totalNanoTime = 0;
        for (int i = 0; i < iterations; i++) {
            long startNano = System.nanoTime();
            RegionQueryResult result = regionQueryService.getTaxisInRegion(query);
            long endNano = System.nanoTime();
            totalNanoTime += (endNano - startNano);
        }

        // 计算平均耗时（毫秒）
        double averageMs = totalNanoTime / (iterations * 1_000_000.0);
        System.out.println("Average query time over " + iterations + " iterations: " + averageMs + " ms");

    }

    /**
     * 测试获取区域内出租车ID列表
     */
    @Test
    public void testGetTaxisInRegion() {
        // 创建区域查询参数（与第一个测试相同区域）
        RegionQuery query = new RegionQuery(
                116.3, // minLongitude
                39.8,  // minLatitude
                116.5, // maxLongitude
                40.0,  // maxLatitude
                LocalDateTime.parse("2008-02-03 11:25:00", formatter),
                LocalDateTime.parse("2008-02-03 11:35:00", formatter)
        );

        //测试获取所有出租车id
        Set<String> taxiIds = regionQueryService.getTaxisInRegion(query)
                .getGpsPoints().stream().map(TaxiRecord::getTaxiId).collect(Collectors.toSet());

        // 验证结果
        assertNotNull(taxiIds, "出租车ID集合不应为空");
        assertFalse(taxiIds.isEmpty(), "出租车ID集合不应为空集合");
        assertTrue(taxiIds.contains("1"), "区域内应包含ID为2的出租车"); // 2,2008-02-03 11:30:43,116.47194,39.90773
        System.out.println("区域内出租车ID: " + taxiIds);
    }

    @AfterEach
    public void cleanUpAfterTest() {
        System.gc();
        System.out.println("测试完成，已执行清理操作。");
    }
}
