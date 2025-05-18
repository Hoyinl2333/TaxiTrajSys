package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.PathFrequency; // 确保这是修改后的 PathFrequency
import lombok.Data;

import java.util.List;

/**
 * 封装频繁路径分析的结果
 */
@Data
public class FrequentPathResult {
    private List<PathFrequency> pathFrequencies;

    public FrequentPathResult(List<PathFrequency> pathFrequencies) {
        this.pathFrequencies = pathFrequencies;
    }
}