package com.codex.taxitrajectory.repository;

import com.codex.taxitrajectory.model.TaxiRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据加载模块，用于加载T-Drive出租车轨迹数据
 * 数据文件存放在classpath下的data文件夹中，每个文件对应一辆出租车
 */
@Component
public class DataLoader {

    // 用于建立出租车ID和数据文件的映射
    private final Map<String, Resource> taxiFileIndex = new HashMap<>();

    // 缓存已加载的数据，键为taxiId，值为按时间排序的轨迹数据(TreeMap)
    private final Map<String, NavigableMap<LocalDateTime, TaxiRecord>> taxiDataCache = new ConcurrentHashMap<>();

    // 通过Spring自动注入资源解析器
    private final ResourcePatternResolver resourcePatternResolver;

    // 从配置文件中读取数据路径，默认为classpath:data/*.txt
    @Value("${taxi.data.path:classpath:data/*.txt}")
    private String dataPath;

    // 时间格式，根据数据格式设置
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DataLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 初始化方法，扫描数据目录并建立出租车文件索引
     */
    @PostConstruct
    public void init() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(dataPath);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".txt")) {
                    // 文件名格式为taxiId.txt，去掉".txt"得到taxiId
                    String taxiId = filename.substring(0, filename.lastIndexOf(".txt"));
                    taxiFileIndex.put(taxiId, resource);
                }
            }
        } catch (IOException e) {
            // 可选日志记录，建议使用日志框架记录错误信息
            System.err.println("扫描数据文件失败：" + e.getMessage());
            throw new RuntimeException("初始化DataLoader失败", e);
        }
    }



    /**
     * 获取指定taxiId对应的轨迹数据
     *
     * @param taxiId 出租车ID
     * @return 按时间排序的轨迹数据，若数据不存在则返回空的TreeMap
     */
    public NavigableMap<LocalDateTime, TaxiRecord> getRecordsByTaxiId(String taxiId) {
        // 利用缓存，若taxiId对应数据尚未加载，调用loadTaxiData()加载数据
        return taxiDataCache.computeIfAbsent(taxiId, this::loadTaxiData);
    }

    /**
     * 获取指定taxiId对应的轨迹记录列表
     *
     * @param taxiId 出租车ID
     * @return 轨迹记录列表
     */
    public List<TaxiRecord> getRecordsByTaxiIdAsList(String taxiId) {
        NavigableMap<LocalDateTime, TaxiRecord> recordMap = getRecordsByTaxiId(taxiId);
        return new ArrayList<>(recordMap.values());
    }


    /**
     * 按时间范围获取指定taxiId的轨迹数据
     *
     * @param taxiId    出租车ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 指定时间范围内的轨迹数据列表
     */
    public List<TaxiRecord> getRecordsByTimeRange(String taxiId, LocalDateTime startTime, LocalDateTime endTime) {
        NavigableMap<LocalDateTime, TaxiRecord> allRecords = getRecordsByTaxiId(taxiId);
        return new ArrayList<>(allRecords.subMap(startTime, true, endTime, true).values());
    }

    /**
     * 获取所有出租车ID列表
     *
     * @return 所有出租车ID的集合
     */
    public Set<String> getAllTaxiIds() {
        return taxiFileIndex.keySet();
    }

    /**
     * 实际加载出租车轨迹数据的方法
     *
     * @param taxiId 出租车ID
     * @return 载入的轨迹数据集合，按时间戳排序
     */
    private NavigableMap<LocalDateTime, TaxiRecord> loadTaxiData(String taxiId) {
        NavigableMap<LocalDateTime, TaxiRecord> records = new TreeMap<>();
        Resource resource = taxiFileIndex.get(taxiId);
        if (resource == null) {
            // 若未找到对应文件，可记录日志后返回空集合
            System.err.println("未找到taxiId: " + taxiId + "对应的数据文件");
            return records;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 每行记录格式：taxi id, date time, longitude, latitude
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    // 格式错误，跳过该行，同时可以记录日志
                    continue;
                }
                try {
                    String id = parts[0].trim();
                    LocalDateTime timestamp = LocalDateTime.parse(parts[1].trim(), formatter);
                    double longitude = Double.parseDouble(parts[2].trim());
                    double latitude = Double.parseDouble(parts[3].trim());
                    TaxiRecord record = new TaxiRecord(id, timestamp, longitude, latitude);
                    records.put(timestamp, record);
                } catch (Exception ex) {
                    // 解析异常，建议记录具体错误信息后跳过此行
                    System.err.println("解析数据错误（taxiId:" + taxiId + "）：" + ex.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载taxiId: " + taxiId + "数据文件失败：" + e.getMessage());
        }
        return records;
    }

    /**
     * 刷新指定taxiId的缓存数据，下次调用时将重新加载数据
     *
     * @param taxiId 出租车ID
     */
    public void refreshCache(String taxiId) {
        taxiDataCache.remove(taxiId);
    }
}
