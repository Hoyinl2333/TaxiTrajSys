package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.Grid;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 密度分析结果类
 */
@Data
public class DensityAnalysisResult {
    // 网格信息直接从前端获取
//    private Double gridSize; // 单位：km
//    private Double minLon,minLat;
//    private Double maxLon,maxLat;

    private Integer rows,cols;

    private List<LocalDateTime> timeSlots;  // 时间槽列表
    private Map<LocalDateTime, Map<String, Integer>> densityMap;  // 密度映射
}
