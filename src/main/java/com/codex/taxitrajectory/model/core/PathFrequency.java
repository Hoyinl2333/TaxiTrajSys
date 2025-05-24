package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.NoArgsConstructor; // 为序列化库（如Jackson）添加无参构造函数
import lombok.AllArgsConstructor; // 为方便对象创建添加全参构造函数

import java.util.List;

/**
 * 表示一条路径及其出现频率。
 * <p>
 * 此类用于封装分析结果，其中路径由一系列地理坐标点 ({@link PointCoordinate}) 表示，
 * 通常是原始路径经过的网格单元的中心点。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathFrequency {

    /**
     * 路径的地理坐标点序列。
     * 每个 {@link PointCoordinate} 对象代表路径上的一个点（例如，网格中心点）。
     */
    private List<PointCoordinate> pathCoordinates;

    /**
     * 该路径出现的频率（次数）。
     */
    private int frequency;

}