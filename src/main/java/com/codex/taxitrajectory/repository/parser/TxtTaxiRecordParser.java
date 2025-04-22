package com.codex.taxitrajectory.repository.parser;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 从本地txt中加载数据
 */
@Component
public class TxtTaxiRecordParser implements TaxiRecordParser {

    private static final Logger log = LoggerFactory.getLogger(TxtTaxiRecordParser.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 日志开关
    @Value("${logging.repository.enable:true}")
    private boolean enableLogging;

    // 阈值常量，用于判断经纬度是否接近0.0，这个值可根据需要进行调整
    private static final double COORDINATE_THRESHOLD = 1e-4;

    @Override
    public NavigableMap<LocalDateTime, TaxiRecord> parse(Resource resource) {
        NavigableMap<LocalDateTime, TaxiRecord> map = new TreeMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    log.warn("行格式错误（跳过）{}: {}", lineNumber, line);
                    continue;
                }

                try {
                    String id = parts[0].trim();
                    LocalDateTime timestamp = LocalDateTime.parse(parts[1].trim(), formatter);
                    double lon = Double.parseDouble(parts[2].trim());
                    double lat = Double.parseDouble(parts[3].trim());

                    // 采用阈值判断过滤异常坐标（例如接近于0.0的值）,只要有一个在阈值内就清洗掉
                    if (Math.abs(lon) < COORDINATE_THRESHOLD || Math.abs(lat) < COORDINATE_THRESHOLD) {
                        continue;
                    }

                    // TODO: 可能会重复时间戳
//                    if (cleanZeroCoordinate && map.containsKey(timestamp)) {
//                        log.info.println("Id:{} 时间戳{}重复，已覆盖原始记录 " ,id, timestamp);
//                    }

                    map.put(timestamp, new TaxiRecord(id, timestamp, lon, lat));
                } catch (Exception e) {
                    if (enableLogging) {
                        log.error("数据解析失败  {}: {}", lineNumber, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (enableLogging){
                log.error("文件解析失败：{}", e.getMessage());
            }
        }
        return map;
    }
}
