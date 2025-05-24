package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.validation.ValidTimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor; // 移除，因为我们提供了自定义构造函数

import java.time.LocalDateTime;

/**
 * 封装F7（全市范围）和F8（区域间）频繁路径分析所需的用户输入参数。
 * <p>
 * 通过JSR 303注解及自定义注解进行参数校验。
 * </p>
 */
@Data
@NoArgsConstructor
@ValidTimeRange(startTimeFieldName = "startTime", endTimeFieldName = "endTime", message = "若提供了时间范围，则结束时间必须在开始时间之后或与开始时间相同")
public class FrequentPathQuery {

    /**
     * 返回的热门路径数量 (Top-K)。
     * 默认为10。必须为正数。
     */
    @NotNull(message = "热门路径数量k不能为空")
    @Min(value = 1, message = "热门路径数量k必须大于0")
    private Integer k = 10;

    /**
     * 路径的最短实际地理距离，单位：千米 (km)。
     * 默认为1.0 km。不能为负数。
     */
    @NotNull(message = "最小路径距离不能为空")
    @DecimalMin(value = "0.0", message = "最小路径距离不能为负数")
    private Double minPathDistanceKM = 1.0;

    /**
     * 分析的可选开始时间。
     * 如果提供，则 {@code endTime} 也应提供。
     */
    private LocalDateTime startTime;

    /**
     * 分析的可选结束时间。
     * 如果提供，则 {@code startTime} 也应提供，且结束时间不能早于开始时间。
     */
    private LocalDateTime endTime;

    /**
     * F8功能专属：起始区域。
     * 如果进行区域间分析 (F8)，此字段和 {@code regionB} 通常都需要提供。
     * 通过 {@link Valid} 注解对其内部字段进行级联校验。
     */
    @Valid
    private Region regionA = null;

    /**
     * F8功能专属：目标区域。
     * 如果进行区域间分析 (F8)，此字段和 {@code regionA} 通常都需要提供。
     * 通过 {@link Valid} 注解对其内部字段进行级联校验。
     */
    @Valid
    private Region regionB = null;

    /**
     * 用于F7（全市范围）查询的构造函数。
     * @param k 返回的热门路径数量。
     * @param minPathDistanceKM 路径的最小地理距离 (km)。
     */
    public FrequentPathQuery(Integer k, Double minPathDistanceKM) {
        if (k != null && k > 0) { // 保留参数校验和默认值逻辑
            this.k = k;
        }
        if (minPathDistanceKM != null && minPathDistanceKM >= 0) {
            this.minPathDistanceKM = minPathDistanceKM;
        }
        // startTime, endTime, regionA, regionB 将使用字段声明时的默认值 (null)
    }

    /**
     * 用于F8（区域间）查询的构造函数。
     * @param k 返回的热门路径数量。
     * @param minPathDistanceKM 路径的最小地理距离 (km)。
     * @param regionA 起始区域。
     * @param regionB 目标区域。
     */
    public FrequentPathQuery(Integer k, Double minPathDistanceKM, Region regionA, Region regionB) {
        this(k, minPathDistanceKM); // 调用上面的构造函数
        this.regionA = regionA;
        this.regionB = regionB;
    }

    /**
     * 辅助方法，判断当前查询是否为区域间查询 (F8)。
     * <p>
     * 此方法主要用于业务逻辑判断，而非核心校验。
     * </p>
     * @return 如果 {@code regionA} 和 {@code regionB} 都非null，则为true。
     */
    public boolean isRegionQuery() {
        return regionA != null && regionB != null;
    }
}