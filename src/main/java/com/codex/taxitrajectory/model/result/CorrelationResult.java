package com.codex.taxitrajectory.model.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 区域关联分析（F5 两区域间, F6 单区域与其它）的结果封装类。
 * <p>
 * 包含一个映射，键为时间槽的起始时间 ({@link LocalDateTime})，
 * 值为一个整型数组，表示该时间槽内的车流量。数组内容的含义取决于具体的分析类型：
 * <ul>
 * <li>对于F5（两区域分析）：通常是 {@code [区域1到区域2的车流量, 区域2到区域1的车流量]}。</li>
 * <li>对于F6（单区域分析）：通常是 {@code [进入指定区域的车流量, 离开指定区域的车流量]}。</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数
@AllArgsConstructor   // 提供全参构造函数
public class CorrelationResult {

    /**
     * 车流量变化数据。
     * 键是时间槽的开始时间，值是一个整型数组，具体含义见类注释。
     */
    private Map<LocalDateTime, int[]> trafficFlowChange;
}