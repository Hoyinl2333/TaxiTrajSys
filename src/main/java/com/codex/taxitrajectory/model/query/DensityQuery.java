package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox;
import com.codex.taxitrajectory.model.validation.ValidTimeRange;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 区域车流密度分析的查询参数。
 * 包含地理边界、时间范围、网格大小和时间槽定义。
 * 通过JSR 303注解及自定义注解进行参数校验。
 */
@Data
@ValidTimeRange // 校验时间范围的有效性
@ValidGeoBoundingBox // 校验地理边界框的有效性
public class DensityQuery {

    /**
     * 网格大小 (公里)。
     */
    @NotNull(message = "网格大小不能为空")
    @Positive(message = "网格大小必须为正数")
    private Double gridSize;

    /**
     * 分析的开始时间。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 分析的结束时间。
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 时间分割粒度 (分钟)。
     */
    @NotNull(message = "时间间隔不能为空")
    @Positive(message = "时间间隔必须为正数")
    private Integer timeSlotMinutes = 60; // 默认60分钟

    /**
     * 查询区域的最小经度。
     */
    @NotNull(message = "最小经度不能为空")
    @DecimalMin(value = "-180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    private Double minLongitude;

    /**
     * 查询区域的最小纬度。
     */
    @NotNull(message = "最小纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    private Double minLatitude;

    /**
     * 查询区域的最大经度。
     */
    @NotNull(message = "最大经度不能为空")
    @DecimalMin(value = "-180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    private Double maxLongitude;

    /**
     * 查询区域的最大纬度。
     */
    @NotNull(message = "最大纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    private Double maxLatitude;

    /**
     * 默认构造函数。
     */
    public DensityQuery() {
    }

    /**
     * 全参数构造函数。
     */
    public DensityQuery(Double gridSize, LocalDateTime startTime, LocalDateTime endTime, Integer timeSlotMinutes,
                        Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude) {
        this.gridSize = gridSize;
        this.startTime = startTime;
        this.endTime = endTime;
        if (timeSlotMinutes != null) {
            this.timeSlotMinutes = timeSlotMinutes;
        }
        this.minLongitude = minLongitude;
        this.minLatitude = minLatitude;
        this.maxLongitude = maxLongitude;
        this.maxLatitude = maxLatitude;
    }
}