package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.PathFrequency;
import lombok.Data;

import java.util.List;

/**
 * Result封装了频繁路径分析结果
 */
@Data
public class FrequentPathResult {
    // 分析得到的热门路径列表
    private List<PathFrequency> pathFrequencies;

    //构造函数
    public FrequentPathResult(List<PathFrequency> pathFrequencies) {
        this.pathFrequencies = pathFrequencies;
    }
}
