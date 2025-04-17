package com.codex.taxitrajectory.repository.parser;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import org.springframework.core.io.Resource;


import java.time.LocalDateTime;
import java.util.NavigableMap;

/**
 * Parser 解析器接口
 * 将原始数据转化为结构化对象
 */
public interface TaxiRecordParser {
    NavigableMap<LocalDateTime, TaxiRecord> parse(Resource resource);
}
