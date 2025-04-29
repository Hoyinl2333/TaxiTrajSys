package com.codex.taxitrajectory.model.result;

 // 引入 TimeInterval 内部类
import com.codex.taxitrajectory.model.core.TimeInterval;
import lombok.Data;

import java.util.Map;

// F9 通行时间分析的总结果。针对整个 F9 查询（可能包含多个时间段）的全部分析结果的总集合。不同于ShortestPathInfo
@Data // 使用 @Data 注解
public class TravelTimeResult {

    // 每个时间段对应的最短路径信息
    private Map<TimeInterval, ShortestPathInfo> results;

    // 保留构造函数
    public TravelTimeResult(Map<TimeInterval, ShortestPathInfo> results) {
        this.results = results;
    }

}