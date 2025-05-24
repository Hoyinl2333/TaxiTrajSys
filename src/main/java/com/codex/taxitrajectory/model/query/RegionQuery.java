package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox;
import com.codex.taxitrajectory.model.validation.ValidTimeRange;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * F3 功能（区域范围查找）的查询参数。
 * <p>
 * 定义了查找特定区域内出租车所需的地理边界和时间范围。
 * 使用JSR 303注解及自定义注解进行参数校验。
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数，主要为框架使用
@AllArgsConstructor   // 提供全参构造函数，方便对象创建
@ValidTimeRange // 校验 startTime 和 endTime 的逻辑关系
@ValidGeoBoundingBox // 校验地理边界框的有效性 (minLongitude, maxLongitude, minLatitude, maxLatitude)
public class RegionQuery {

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

    /**
     * 查询的开始时间。
     * 不能为空。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 查询的结束时间。
     * 不能为空，并且必须在开始时间之后或与开始时间相同（由 {@link ValidTimeRange} 校验）。
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

}