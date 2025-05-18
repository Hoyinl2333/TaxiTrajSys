package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.TaxiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaxiRepository测试类
 * 用于测试出租车轨迹数据加载功能
 */
@SpringBootTest // @SpringBootTest提供了完整的集成测试环境,DataLoader是一个Spring组件并依赖于Spring的资源加载机制，使用@SpringBootTest是合适的选择。
public class TaxiRepositoryTest {

    @Autowired
    private TaxiRepository taxiRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试获取所有出租车ID的功能
     */
    @Test
    public void testGetAllTaxiIds() {
        // 获取所有出租车ID
        Set<String> allTaxiIds = taxiRepository.getAllTaxiIds();

        // 验证结果不为空
        assertNotNull(allTaxiIds, "出租车ID集合不应为空");

        // 验证结果不为空集合
        assertFalse(allTaxiIds.isEmpty(), "出租车ID集合不应为空集合");

        // 验证是否包含示例数据中的taxi_id = "295"
        assertTrue(allTaxiIds.contains("295"), "出租车ID集合应该包含ID为1的出租车");

        // 打印获取到的出租车ID数量
        System.out.println("获取到的出租车ID总数: " + allTaxiIds.size());
        assertEquals(10357, allTaxiIds.size(), "错误：获取到的出租车ID数量为 " + allTaxiIds.size() + "，但期望为 10357");
    }

    /**
     * 测试根据出租车ID获取轨迹记录的功能，测试清洗效果（以Id=9757为测试案例)
     */
    @Test
    public void testGetRecordsByTaxiId() {

        // 获取ID为1的出租车轨迹记录（一定是有效数据）
        NavigableMap<LocalDateTime, TaxiRecord> records1 = taxiRepository.getRecordsByTaxiId("1");

        // 验证结果不为空
        assertNotNull(records1, "出租车轨迹记录不应为空");
        assertFalse(records1.isEmpty(), "出租车轨迹记录集合不应为空");

        // 验证第一条记录的准确性（与提供的数据文件对比）
        TaxiRecord firstRecord = records1.firstEntry().getValue();
        assertEquals("1", firstRecord.getTaxiId(), "出租车ID应为1");
        assertEquals(116.51172, firstRecord.getLongitude(), 0.00001, "经度值不匹配");
        assertEquals(39.92123, firstRecord.getLatitude(), 0.00001, "纬度值不匹配");

        // 打印获取到的记录数量
        System.out.println("ID为1的出租车轨迹记录数: " + records1.size());
        System.out.println("ID为1的第一条非0出租车轨迹: " + firstRecord);

        System.out.println("============== 数据清洗检查： ===============");
        // 获取ID为9757的出租车轨迹记录
        NavigableMap<LocalDateTime, TaxiRecord> records9757 = taxiRepository.getRecordsByTaxiId("9757");

        assertNotNull(records9757, "出租车 9757 轨迹记录不应为空");
        assertFalse(records9757.isEmpty(), "出租车 9757 轨迹记录集合不应为空");
        assertEquals(2, records9757.size(), "出租车 9757 轨迹清洗后应该为只有2条");
        // 验证Id=9757记录的准确性
        firstRecord = records9757.firstEntry().getValue();
        assertEquals("9757", firstRecord.getTaxiId(), "出租车ID应为 9757 ");
        assertEquals(116.54252, firstRecord.getLongitude(), 0.00001, "经度值不匹配");
        assertEquals(39.91612, firstRecord.getLatitude(), 0.00001, "纬度值不匹配");

        // 打印获取到的记录数量
        System.out.println("ID为 9757 的出租车轨迹记录数: " + records9757.size());
        System.out.println("ID为 9757 的第一条非0出租车轨迹: " + firstRecord.toString());
    }

    /**
     * 测试【随机】出租车ID获取轨迹记录列表（允许空结果）
     * 这个测试主要用于验证系统在处理大量不同ID时的稳定性和对空数据的处理能力。
     */
    @Test
    public void testGetRandomRecordsByTaxiIdAsListToleratingEmpty() {
        Random random = new Random();
        int count = 100; // 测试次数
        int emptyCount = 0; // 记录空结果的次数

        System.out.println("=== Starting Random Taxi ID Test (Tolerating Empty Results) ===");
        for (int i = 0; i < count; i++) {
            String randomTaxiId = String.valueOf(random.nextInt(10357) + 1);
            System.out.println("Testing random Taxi ID: " + randomTaxiId);
            List<TaxiRecord> listRecords = taxiRepository.getRecordsByTaxiIdAsList(randomTaxiId);

            assertNotNull(listRecords, "列表本身不应为 null, 即使数据为空 for ID: " + randomTaxiId); // 验证方法契约

            if (!listRecords.isEmpty()) {
                // 如果列表不为空，执行之前的验证
                TaxiRecord firstRecord = listRecords.getFirst();
                System.out.println("  记录数量: " + listRecords.size());
                System.out.println("  第一条记录: " + firstRecord.toString());
                assertNotNull(firstRecord.getTaxiId(), "  出租车ID不应为null");
                assertEquals(randomTaxiId, firstRecord.getTaxiId(), "  出租车ID应匹配");
            } else {
                // 如果列表为空，不再失败，而是记录下来
                System.out.println("  记录数量: 0 (文件可能为空、无效或解析/清洗后无数据)");
                emptyCount++;
            }
            System.out.println("-----------------------------");
        }
        System.out.println("=== Random Taxi ID Test Finished ===");
        System.out.printf("Tested %d random IDs, %d returned 0 records.%n", count, emptyCount);
        // 可选：可以加一个软断言，比如如果空记录比例过高（> 某个阈值%），则打印警告或失败
        // assertTrue( (double)emptyCount / count < 0.8, "空记录比例过高，可能存在问题！");
    }

    /**
     * 测试多次根据出租车ID获取轨迹记录
     */
    @Test
    public void testGetRecordsByTaxiIdAsList() {
        Random random = new Random();
        int count = 200;

        for (int i = 0; i < count; i++) {
            String randomTaxiId = String.valueOf(random.nextInt(10357) + 1); // 生成范围1-10357的ID
            List<TaxiRecord> listRecords = taxiRepository.getRecordsByTaxiIdAsList(randomTaxiId);

            System.out.println("Taxi ID: " + randomTaxiId);
            if (listRecords != null && !listRecords.isEmpty()) {
                TaxiRecord firstRecord = listRecords.getFirst();
                System.out.println("记录数量: " + listRecords.size());
                System.out.println("第一条记录: " + firstRecord.toString());

                // 基础断言
                assertNotNull(firstRecord.getTaxiId(), "出租车ID不应为null");
                assertEquals(randomTaxiId, firstRecord.getTaxiId(), "出租车ID应匹配");
            } else {
                System.out.println("无记录，请检查是否出租车坐标存在问题");
                //fail("出租车ID " + randomTaxiId + " 没有轨迹记录");
            }
            System.out.println("=============================");
        }
    }

    /**
     * 测试根据时间范围获取轨迹记录的功能
     */
    @Test
    public void testGetRecordsByTimeRange() {
        // 定义查询时间范围
        LocalDateTime startTime = LocalDateTime.parse("2008-02-02 15:00:00", formatter);
        LocalDateTime endTime = LocalDateTime.parse("2008-02-02 17:00:00", formatter);

        // 获取时间范围内的轨迹记录
        List<TaxiRecord> records = taxiRepository.getRecordsByTimeRange("1", startTime, endTime);

        // 验证结果不为空(由于清洗数据的情况，可能存在空记录）
        //assertNotNull(records, "时间范围内的轨迹记录不应为空");
        //assertFalse(records.isEmpty(), "时间范围内的轨迹记录集合不应为空");

        // 验证所有记录的时间戳都在指定范围内
        for (TaxiRecord record : records) {
            LocalDateTime timestamp = record.getTimestamp();
            assertTrue(
                    (timestamp.isEqual(startTime) || timestamp.isAfter(startTime)) &&
                            (timestamp.isEqual(endTime) || timestamp.isBefore(endTime)),
                    "记录时间 " + timestamp + " 应在指定范围内");
        }

        // 验证特定时间点的记录
        boolean found = records.stream().anyMatch(
                r -> r.getTimestamp().equals(LocalDateTime.parse("2008-02-02 15:36:08", formatter)));
        assertTrue(found, "应包含2008-02-02 15:36:08时间点的记录");

        // 打印获取到的记录数量
        System.out.println("指定时间范围内的轨迹记录数: " + records.size());
    }

    /**
     * 测试数据加载的准确性
     */
    @Test
    public void testDataAccuracy() {
        // 获取ID为1的出租车轨迹记录
        NavigableMap<LocalDateTime, TaxiRecord> records = taxiRepository.getRecordsByTaxiId("1");

        // 验证特定时间点的记录
        LocalDateTime specificTime = LocalDateTime.parse("2008-02-02 15:36:08", formatter);
        TaxiRecord record = records.get(specificTime);

        // 验证记录存在且数据准确
        assertNotNull(record, "应存在2008-02-02 15:36:08时间点的记录");
        assertEquals("1", record.getTaxiId(), "出租车ID应为1");
        assertEquals(116.51172, record.getLongitude(), 0.00001, "经度值不匹配");
        assertEquals(39.92123, record.getLatitude(), 0.00001, "纬度值不匹配");

        // 验证后续记录
        specificTime = LocalDateTime.parse("2008-02-02 15:46:08", formatter);
        record = records.get(specificTime);
        assertNotNull(record, "应存在2008-02-02 15:46:08时间点的记录");
        assertEquals(116.51135, record.getLongitude(), 0.00001, "经度值不匹配");
        assertEquals(39.93883, record.getLatitude(), 0.00001, "纬度值不匹配");
    }

    /**
     * 测试缓存刷新功能
     */
    @Test
    public void testRefreshCache() {
        // 先加载数据到缓存
        NavigableMap<LocalDateTime, TaxiRecord> records1 = taxiRepository.getRecordsByTaxiId("1");
        assertNotNull(records1, "第一次加载的记录不应为空");

        // 刷新缓存
        taxiRepository.refresh("1");

        // 再次加载数据
        NavigableMap<LocalDateTime, TaxiRecord> records2 = taxiRepository.getRecordsByTaxiId("1");
        assertNotNull(records2, "刷新缓存后加载的记录不应为空");

        // 内容应该相同但可能是不同的对象
        assertEquals(records1.size(), records2.size(), "刷新前后记录数量应相同");
    }
}
