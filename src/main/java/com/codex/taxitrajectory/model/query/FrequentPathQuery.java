package com.codex.taxitrajectory.model.query;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * FrequentPathQuery 封装F7频繁路径分析所需参数，
 * 除了原来的 k（返回热门路径个数）和 minDistance（路径最小累计距离）外，
 * 还增加了查询时间范围 startTime 与 endTime。
 */
@Data
public class FrequentPathQuery {
    // 返回热门路径数量
    private int k;
    // 有效路径的最小累计距离（单位：km）
    private double minDistance;
    // 查询的起始时间
    private LocalDateTime startTime;
    // 查询的结束时间
    private LocalDateTime endTime;
}
