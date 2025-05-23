package com.codex.taxitrajectory.model.core;

import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox; // 导入自定义注解
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 表示一个矩形地理区域。
 * 通过JSR 303注解及自定义注解 {@link ValidGeoBoundingBox} 进行参数校验。
 */
@Data
@ValidGeoBoundingBox(
        minLonFieldName = "minLon",
        maxLonFieldName = "maxLon",
        minLatFieldName = "minLat",
        maxLatFieldName = "maxLat"
)
public class Region {

    /**
     * 区域的最小纬度。
     * 必须在 [-90, 90] 范围内，且小于最大纬度。
     */
    @NotNull(message = "最小纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    private Double minLat;

    /**
     * 区域的最大纬度。
     * 必须在 [-90, 90] 范围内，且大于最小纬度。
     */
    @NotNull(message = "最大纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    private Double maxLat;

    /**
     * 区域的最小经度。
     * 必须在 [-180, 180] 范围内，且小于最大经度。
     */
    @NotNull(message = "最小经度不能为空")
    @DecimalMin(value = "-180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    private Double minLon;

    /**
     * 区域的最大经度。
     * 必须在 [-180, 180] 范围内，且大于最小经度。
     */
    @NotNull(message = "最大经度不能为空")
    @DecimalMin(value = "-180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    private Double maxLon;

    /**
     * 默认构造函数，供框架使用。
     */
    public Region() {
    }

    /**
     * 全参数构造函数。
     * 注意参数顺序与字段声明顺序可能不同，这里遵循了您原有的构造函数参数顺序。
     *
     * @param minLon 最小经度
     * @param minLat 最小纬度
     * @param maxLon 最大经度
     * @param maxLat 最大纬度
     */
    public Region(double minLon, double minLat, double maxLon, double maxLat) {
        this.minLon = minLon; // 注意：这里直接赋值，JSR 303校验会在对象构建后，由@Valid触发时执行
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
    }
}