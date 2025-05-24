package com.codex.taxitrajectory.model.result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 区域车流密度分析 (F4) 的结果封装类。
 * <p>
 * 包含分析生成的网格维度信息、时间槽列表以及每个时间槽对应的密度数据。
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数
@AllArgsConstructor   // 提供全参构造函数
public class DensityAnalysisResult {


    /**
     * 分析所用网格的总行数。
     */
    private Integer rows;

    /**
     * 分析所用网格的总列数。
     */
    private Integer cols;

    /**
     * 按时间顺序排列的时间槽列表。
     * 每个时间槽代表一个分析时间点。
     */
    private List<LocalDateTime> timeSlots;

    /**
     * 车流密度数据映射。
     * <p>
     * 键为时间槽 ({@link LocalDateTime})，值为另一个映射。
     * 内部映射的键为网格单元格的ID（通常格式为 "row,col"），值为该单元格在该时间槽内的车辆密度计数。
     * </p>
     */
    private Map<LocalDateTime, Map<String, Integer>> densityMap;
}