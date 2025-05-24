package com.codex.taxitrajectory.model.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示一个地理空间中的点，由经度和纬度定义。
 * <p>
 * 此类通常用于表示路径上的具体坐标点，例如网格单元的中心点或原始GPS记录点。
 * </p>
 */
@Data
@NoArgsConstructor    // 为序列化/反序列化库（如Jackson）提供无参构造函数
@AllArgsConstructor   // 提供包含所有字段的构造函数
public class PointCoordinate {

    /**
     * 地理点的经度值。
     */
    private double longitude;

    /**
     * 地理点的纬度值。
     */
    private double latitude;

}