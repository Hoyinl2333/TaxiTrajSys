package com.codex.taxitrajectory.model.core;

import lombok.Data;

import java.time.LocalDateTime;

// 表示一个时间段
@Data // 使用 @Data 注解自动生成 getter, setter, equals, hashCode, toString
public class TimeInterval {
    private LocalDateTime startTime; // 时间段开始时间
    private LocalDateTime endTime;   // 时间段结束时间

    // 保留构造函数
    public TimeInterval(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

}