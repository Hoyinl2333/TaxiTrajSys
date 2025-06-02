package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.validation.ValidTimeRange; // 导入自定义时间范围注解
import jakarta.validation.Valid; // 导入 @Valid 用于级联校验
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull; // 根据需要，如果k和minPathDistanceKM是必需的
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 封装F7（全市范围）和F8（区域间）频繁路径分析所需的用户输入参数。
 * 通过JSR 303注解及自定义注解进行参数校验。
 */
@Data
@NoArgsConstructor
@ValidTimeRange(startTimeFieldName = "startTime", endTimeFieldName = "endTime", message = "若提供了时间范围，则结束时间必须在开始时间之后或与开始时间相同")
public class FrequentPathQuery {

    /**
     * 返回的热门路径数量 (Top-K)。
     * 如果在请求中未指定，默认为10。必须为正数。
     */
    @NotNull(message = "热门路径数量k不能为空") // k通常是必需的
    @Min(value = 1, message = "热门路径数量k必须大于0")
    private Integer k = 10; // 改为Integer以允许@NotNull，如果允许不传则可为int并依赖默认值

    /**
     * 路径的最短实际地理距离，单位：千米 (km)。
     * 如果在请求中未指定，默认为1.0 km。不能为负数。
     */
    @NotNull(message = "最小路径距离不能为空") // minPathDistanceKM通常是必需的
    @DecimalMin(value = "0.0", message = "最小路径距离不能为负数")
    private Double minPathDistanceKM = 1.0; // 改为Double以允许@NotNull

    /**
     * 分析的可选开始时间。
     * 如果提供，则 {@code endTime} 也应提供。
     */
    private LocalDateTime startTime;

    /**
     * 分析的可选结束时间。
     * 如果提供，则 {@code startTime} 也应提供。
     */
    private LocalDateTime endTime;

    /**
     * F8功能专属：起始区域。
     * 如果进行区域间分析 (F8)，此字段和 {@code regionB} 通常都需要提供。
     * 通过 {@link Valid} 注解对其内部字段进行级联校验。
     */
    @Valid // 当regionA非null时，会校验Region对象内部的约束
    private Region regionA = null;

    /**
     * F8功能专属：目标区域。
     * 如果进行区域间分析 (F8)，此字段和 {@code regionA} 通常都需要提供。
     * 通过 {@link Valid} 注解对其内部字段进行级联校验。
     */
    @Valid // 当regionB非null时，会校验Region对象内部的约束
    private Region regionB = null;

    /**
     * 用于F7（全市范围）查询的构造函数。
     * @param k 返回的热门路径数量。
     * @param minPathDistanceKM 路径的最小地理距离 (km)。
     */
    public FrequentPathQuery(Integer k, Double minPathDistanceKM) {
        if (k != null && k > 0) {
            this.k = k;
        }
        if (minPathDistanceKM != null && minPathDistanceKM >= 0) {
            this.minPathDistanceKM = minPathDistanceKM;
        }
        // startTime, endTime, regionA, regionB 默认为 null
    }

    /**
     * 用于F8（区域间）查询的构造函数。
     * @param k 返回的热门路径数量。
     * @param minPathDistanceKM 路径的最小地理距离 (km)。
     * @param regionA 起始区域。
     * @param regionB 目标区域。
     */
    public FrequentPathQuery(Integer k, Double minPathDistanceKM, Region regionA, Region regionB) {
        this(k, minPathDistanceKM);
        this.regionA = regionA;
        this.regionB = regionB;
    }

    /**
     * 辅助方法，判断当前查询是否为区域间查询 (F8)。
     * 此方法主要用于业务逻辑判断，而非核心校验。
     * @return 如果 {@code regionA} 和 {@code regionB} 都非null，则为true。
     */
    public boolean isRegionQuery() {
        return regionA != null && regionB != null;
    }

    // isValid() 方法已被移除。
    // 对于F8查询时，regionA和regionB是否必须存在的校验：
    // 如果API设计为特定端点总是F8，那么Controller可以在调用服务前检查这两个字段是否为null。
    // 或者，可以使用JSR 303的校验组(groups)功能，为F8查询定义一个组，
    // 然后在regionA和regionB上添加@NotNull(groups = F8.class)。
    // 目前的简化做法是，如果客户端意图是F8查询，它应该提供这两个区域；如果未提供，
    // isRegionQuery()会返回false，服务层逻辑会按F7（或报错，取决于具体实现）处理。
    // @Valid 会确保如果regionA/regionB被提供了，它们内部是有效的。
}