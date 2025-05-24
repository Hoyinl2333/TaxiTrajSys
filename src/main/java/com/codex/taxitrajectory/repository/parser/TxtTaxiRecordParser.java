package com.codex.taxitrajectory.repository.parser;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class TxtTaxiRecordParser implements TaxiRecordParser {

    private static final Logger log = LoggerFactory.getLogger(TxtTaxiRecordParser.class);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${logging.repository.enabled:true}")
    private boolean enableLogging;
    private static final double COORDINATE_THRESHOLD = 1e-4;

    @Override
    public NavigableMap<LocalDateTime, TaxiRecord> parse(Resource resource) throws IOException {
        NavigableMap<LocalDateTime, TaxiRecord> map = new TreeMap<>();
        // try-with-resources 确保 BufferedReader 被关闭
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    if (enableLogging) log.warn("行格式错误（跳过）{}: {}", lineNumber, line);
                    continue;
                }
                try {
                    String id = parts[0].trim();
                    LocalDateTime timestamp = LocalDateTime.parse(parts[1].trim(), formatter);
                    double lon = Double.parseDouble(parts[2].trim());
                    double lat = Double.parseDouble(parts[3].trim());
                    if (Math.abs(lon) < COORDINATE_THRESHOLD || Math.abs(lat) < COORDINATE_THRESHOLD) {
                        continue;
                    }
                    map.put(timestamp, new TaxiRecord(id, timestamp, lon, lat));
                } catch (Exception e) {
                    if (enableLogging) {
                        log.error("数据解析失败 {}: {} at line: {}", resource.getFilename(), e.getMessage(), lineNumber);
                    }
                }
            }
        } catch (IOException e) {
            if (enableLogging) {
                log.error("文件读取失败：{} for resource: {}", e.getMessage(), resource.getFilename());
            }
            throw e;
        }
        return map;
    }

    // 流式解析方法
    @Override
    public Stream<TaxiRecord> parseAsStream(Resource resource) throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            if (enableLogging) {
                log.error("无法打开资源进行流式解析: {} - {}", resource.getFilename(), e.getMessage());
            }
            throw e;
        }

        Iterator<TaxiRecord> iterator = new Iterator<>() {
            private String nextLine = null;
            private int lineNumber = 0;
            private TaxiRecord nextRecord = null;
            private boolean finished = false;

            private void fetchNextRecord() {
                if (nextRecord != null || finished) {
                    return;
                }
                try {
                    while ((nextLine = reader.readLine()) != null) {
                        lineNumber++;
                        String[] parts = nextLine.split(",");
                        if (parts.length != 4) {
                            if (enableLogging) log.warn("流式解析：行格式错误（跳过）{}: {}", lineNumber, nextLine);
                            continue; // 跳过格式错误的行，继续尝试下一行
                        }
                        try {
                            String id = parts[0].trim();
                            LocalDateTime timestamp = LocalDateTime.parse(parts[1].trim(), formatter);
                            double lon = Double.parseDouble(parts[2].trim());
                            double lat = Double.parseDouble(parts[3].trim());

                            if (Math.abs(lon) < COORDINATE_THRESHOLD || Math.abs(lat) < COORDINATE_THRESHOLD) {
                                continue; // 跳过无效坐标的行
                            }
                            nextRecord = new TaxiRecord(id, timestamp, lon, lat);
                            return; // 找到了一个有效的记录
                        } catch (Exception e) {
                            if (enableLogging) {
                                log.error("流式解析：数据解析失败 {}: {} at line: {}", resource.getFilename(), e.getMessage(), lineNumber);
                            }
                            // 继续尝试解析下一行
                        }
                    }
                    // 没有更多行了
                    finished = true;
                    closeReader();
                } catch (IOException e) {
                    if (enableLogging) {
                        log.error("流式解析：文件读取失败 {}: {}", resource.getFilename(), e.getMessage());
                    }
                    finished = true;
                    closeReader();
                }
            }

            @Override
            public boolean hasNext() {
                if (finished && nextRecord == null) return false;
                if (nextRecord == null) {
                    fetchNextRecord();
                }
                return nextRecord != null;
            }

            @Override
            public TaxiRecord next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more records to parse.");
                }
                TaxiRecord recordToReturn = nextRecord;
                nextRecord = null; // 消费掉
                return recordToReturn;
            }

            private void closeReader() {
                try {
                    reader.close();
                } catch (IOException e) {
                    if (enableLogging) {
                        log.error("流式解析：关闭reader失败 {}: {}", resource.getFilename(), e.getMessage());
                    }
                }
            }
        };

        return StreamSupport.stream(iteratorToSpliterator(iterator), false).onClose(() -> {
            try {
                reader.close();
            } catch (IOException e) {
                if (enableLogging) {
                    log.error("流式解析：关闭reader (onClose)失败 {}: {}", resource.getFilename(), e.getMessage());
                }
            }
        });
    }

    // 辅助方法将Iterator转换为Spliterator
    private static <T> java.util.Spliterator<T> iteratorToSpliterator(Iterator<T> iterator) {
        return java.util.Spliterators.spliteratorUnknownSize(iterator, java.util.Spliterator.ORDERED);
    }
}