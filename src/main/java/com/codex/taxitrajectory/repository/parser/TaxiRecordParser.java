package com.codex.taxitrajectory.repository.parser;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import org.springframework.core.io.Resource;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.NavigableMap;
import java.util.stream.Stream;

/**
 * Parser 解析器接口
 * 将原始数据转化为结构化对象
 */
public interface TaxiRecordParser {
    // 保留旧方法，一次性加载数据
    NavigableMap<LocalDateTime, TaxiRecord> parse(Resource resource)throws IOException; // 声明 IOException

    // 新增方法，流式解析方法
    Stream<TaxiRecord> parseAsStream(Resource resource) throws IOException; // 声明 IOException

}
