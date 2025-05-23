package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox;
import com.codex.taxitrajectory.model.validation.ValidTimeRange;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * F3功能（区域范围查找）的查询参数。
 * 定义了查找特定区域内出租车所需的地理边界和时间范围。
 * 使用JSR 303注解及自定义注解进行参数校验。
 */
@Data
@ValidTimeRange // 校验时间范围的有效性
@ValidGeoBoundingBox // 校验地理边界框的有效性
public class RegionQuery {

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
     * 查询的开始时间。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 查询的结束时间。
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 默认构造函数。
     */
    public RegionQuery() {
    }

    /**
     * 全参数构造函数。
     * @param minLongitude 最小经度
     * @param minLatitude  最小纬度
     * @param maxLongitude 最大经度
     * @param maxLatitude  最大纬度
     * @param startTime    开始时间
     * @param endTime      结束时间
     */
    public RegionQuery(Double minLongitude, Double minLatitude, Double maxLongitude,
                       Double maxLatitude, LocalDateTime startTime, LocalDateTime endTime) {
        this.minLongitude = minLongitude;
        this.minLatitude = minLatitude;
        this.maxLongitude = maxLongitude;
        this.maxLatitude = maxLatitude;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}