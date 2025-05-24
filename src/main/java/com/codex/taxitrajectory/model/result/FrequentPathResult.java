package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.PathFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 封装频繁路径分析（F7 全市范围, F8 区域间）的结果。
 * <p>
 * 包含一个 {@link PathFrequency} 对象列表，每个对象代表一条频繁路径及其出现频率。
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数
@AllArgsConstructor   // 提供全参构造函数
public class FrequentPathResult {

    /**
     * 频繁路径及其频率的列表。
     * 列表中的路径按频率降序排列。
     */
    private List<PathFrequency> pathFrequencies;

}