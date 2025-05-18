package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * FrequentPathQuery 封装 F7/F8 频繁路径分析所需的用户输入参数.
 */
@Data
@NoArgsConstructor
public class FrequentPathQuery {

    // 用户指定参数
    private int k = 10; // 返回热门路径数量

    private double minPathDistanceKM = 1.0; // 路径的最短实际地理距离 (km) - 用户输入的 'x'

    // 可选的时间范围
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // F8 专属参数，默认null
    private Region regionA = null;
    private Region regionB = null;

    // --- 自定义构造函数 ---

    /**
     * F7 查询的构造函数 (不包含区域和时间).
     * @param k 返回热门路径数量.
     * @param minPathDistanceKM 最小距离 (米).
     */
    public FrequentPathQuery(int k, double minPathDistanceKM) {
        this.k = (k > 0) ? k : 10; // 在构造函数中处理默认值或基本校验
        this.minPathDistanceKM = (minPathDistanceKM > 0) ? minPathDistanceKM : 1; // 处理默认值
        // regionA, regionB, startTime, endTime 默认为 null
    }


    /**
     * F8 查询的构造函数 (包含区域，不包含时间).
     * @param k 返回热门路径数量.
     * @param minPathDistanceKM 最小距离 (千米).
     * @param regionA 起始区域.
     * @param regionB 目标区域.
     */
    public FrequentPathQuery(int k, double minPathDistanceKM, Region regionA, Region regionB) {
        this(k, minPathDistanceKM); // 调用基础构造函数
        this.regionA = regionA;
        this.regionB = regionB;
    }



    // --- 辅助方法 ---
    public boolean isRegionQuery() {
        return regionA != null && regionB != null;
    }

    /**
     * 参数校验方法 (校验用户输入的部分).
     */
    public boolean isValid() {
        if (k <= 0 || minPathDistanceKM < 0) { // 只校验用户输入的 k 和 distance
            return false;
        }
        if (isRegionQuery()) {
            if (regionA == null || regionB == null ) {
                return false;
            }
        }
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            return false;
        }
        return true;
    }
}