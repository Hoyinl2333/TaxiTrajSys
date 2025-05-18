package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TimeInterval; // 引入独立的 TimeInterval 类
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// F9 通行时间分析的查询参数
@Data // 使用 @Data 注解
public class TravelTimeQuery {

    private Region regionA; // 起始区域 A
    private Region regionB; // 结束区域 B
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public TravelTimeQuery(Region regionA, Region regionB, LocalDateTime startTime, LocalDateTime endTime) {
        this.regionA = regionA;
        this.regionB = regionB;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}