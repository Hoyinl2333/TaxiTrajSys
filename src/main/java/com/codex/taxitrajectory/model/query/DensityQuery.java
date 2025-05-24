package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox;
import com.codex.taxitrajectory.model.validation.ValidTimeRange;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 区域车流密度分析 (F4) 的查询参数。
 * <p>
 * 包含地理边界、时间范围、网格大小和时间槽定义。
 * 通过JSR 303注解及自定义注解进行参数校验。
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数
@AllArgsConstructor   // 提供全参构造函数
@ValidTimeRange // 校验 startTime 和 endTime 的逻辑关系
@ValidGeoBoundingBox // 校验地理边界框的有效性 (minLongitude, maxLongitude, minLatitude, maxLatitude)
public class DensityQuery {

    /**
     * 网格大小（公里）。
     * 必须提供且为正数。
     */
    @NotNull(message = "网格大小不能为空")
    @Positive(message = "网格大小必须为正数")
    private Double gridSize;

    /**
     * 分析的开始时间。
     * 不能为空。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 分析的结束时间。
     * 不能为空，并且必须在开始时间之后或与开始时间相同（由 {@link ValidTimeRange} 校验）。
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 时间分割粒度（分钟）。
     * 必须提供且为正数，默认为60分钟。
     */
    @NotNull(message = "时间间隔不能为空")
    @Positive(message = "时间间隔必须为正数")
    private Integer timeSlotMinutes = 60; // 默认值60分钟

    /**
     * 查询区域的最小经度。
     * 必须提供，且为有效的地理坐标值 [-180.0, 180.0]。
     * 还需满足 minLongitude < maxLongitude (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最小经度不能为空")
    @DecimalMin(value = "-180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    private Double minLongitude;

    /**
     * 查询区域的最小纬度。
     * 必须提供，且为有效的地理坐标值 [-90.0, 90.0]。
     * 还需满足 minLatitude < maxLatitude (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最小纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    private Double minLatitude;

    /**
     * 查询区域的最大经度。
     * 必须提供，且为有效的地理坐标值 [-180.0, 180.0]。
     * 还需满足 minLongitude < maxLongitude (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最大经度不能为空")
    @DecimalMin(value = "-180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    private Double maxLongitude;

    /**
     * 查询区域的最大纬度。
     * 必须提供，且为有效的地理坐标值 [-90.0, 90.0]。
     * 还需满足 minLatitude < maxLatitude (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最大纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    private Double maxLatitude;

}