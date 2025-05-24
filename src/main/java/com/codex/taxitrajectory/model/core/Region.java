package com.codex.taxitrajectory.model.core;

import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示一个矩形地理区域。
 * <p>
 * 此类定义了一个地理边界框，通过最小/最大经纬度来确定。
 * 使用 JSR 303 标准注解及自定义注解 {@link ValidGeoBoundingBox} 对地理边界的有效性进行参数校验。
 * </p>
 */
@Data
@NoArgsConstructor    // 为框架（如Jackson反序列化）提供无参构造函数
@AllArgsConstructor   // 提供所有字段的构造函数 (替代手写构造函数)
@ValidGeoBoundingBox( // 类级别校验，确保经纬度间的逻辑关系正确
        minLonFieldName = "minLon",
        maxLonFieldName = "maxLon",
        minLatFieldName = "minLat",
        maxLatFieldName = "maxLat"
)
public class Region {

    /**
     * 区域的最小纬度。
     * 必须提供，且值必须在 [-90.0, 90.0] 的有效地理纬度范围内。
     * 还需满足 minLat < maxLat (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最小纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最小纬度必须是有效的地理坐标值 [-90, 90]")
    private Double minLat;

    /**
     * 区域的最大纬度。
     * 必须提供，且值必须在 [-90.0, 90.0] 的有效地理纬度范围内。
     * 还需满足 minLat < maxLat (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最大纬度不能为空")
    @DecimalMin(value = "-90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    @DecimalMax(value = "90.0", message = "最大纬度必须是有效的地理坐标值 [-90, 90]")
    private Double maxLat;

    /**
     * 区域的最小经度。
     * 必须提供，且值必须在 [-180.0, 180.0] 的有效地理经度范围内。
     * 还需满足 minLon < maxLon (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最小经度不能为空")
    @DecimalMin(value = "-180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最小经度必须是有效的地理坐标值 [-180, 180]")
    private Double minLon;

    /**
     * 区域的最大经度。
     * 必须提供，且值必须在 [-180.0, 180.0] 的有效地理经度范围内。
     * 还需满足 minLon < maxLon (由 {@link ValidGeoBoundingBox} 校验)。
     */
    @NotNull(message = "最大经度不能为空")
    @DecimalMin(value = "-180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    @DecimalMax(value = "180.0", message = "最大经度必须是有效的地理坐标值 [-180, 180]")
    private Double maxLon;


}