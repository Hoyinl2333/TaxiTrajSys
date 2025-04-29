package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TimeInterval; // 引入独立的 TimeInterval 类
import lombok.Data;

import java.util.List;

// F9 通行时间分析的查询参数
@Data // 使用 @Data 注解
public class TravelTimeQuery {

    private Region regionA; // 起始区域 A
    private Region regionB; // 结束区域 B
    private List<TimeInterval> timeIntervals; // 需要分析的时间段列表（可以只有一个元素）

    // 保留构造函数
    public TravelTimeQuery(Region regionA, Region regionB, List<TimeInterval> timeIntervals) {
        this.regionA = regionA;
        this.regionB = regionB;
        this.timeIntervals = timeIntervals;
    }
}