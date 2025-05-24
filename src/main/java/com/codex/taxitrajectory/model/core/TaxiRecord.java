package com.codex.taxitrajectory.model.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 表示一条出租车的GPS轨迹记录。
 * <p>
 * 封装了出租车在特定时间点的标识、时间戳以及经纬度坐标。
 * </p>
 */
@Data
@NoArgsConstructor   // 为可能的序列化/反序列化或框架使用提供无参构造函数
@AllArgsConstructor  // 提供所有字段的构造函数
public class TaxiRecord {

    /**
     * 出租车的唯一标识符。
     */
    private String taxiId;

    /**
     * 该GPS记录的时间戳。
     */
    private LocalDateTime timestamp;

    /**
     * GPS记录点的经度。
     * 使用 {@link Double} 类型以支持潜在的 null 值（尽管通常期望非空）。
     */
    private Double longitude;

    /**
     * GPS记录点的纬度。
     * 使用 {@link Double} 类型以支持潜在的 null 值（尽管通常期望非空）。
     */
    private Double latitude;

}