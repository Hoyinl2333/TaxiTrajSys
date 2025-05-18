package com.codex.taxitrajectory.model.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 车流密度分析查询参数类
 * 用于F4功能：区域车流密度分析
 */
@Data
public class DensityQuery {
    @Positive(message = "网格大小必须为正数")
    private Double gridSize;  // 网格大小(r)，单位：公里

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime; // 开始时间

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;   // 结束时间

    @Positive(message = "时间间隔必须为正数")
    private Integer timeSlotMinutes = 60; // 时间分割粒度，默认为60分钟

    // 可选参数：区域范围，默认为北京市区范围
    private Double minLongitude = 116.0;
    private Double minLatitude = 39.6;
    private Double maxLongitude = 117.0;
    private Double maxLatitude = 40.2;

    /**
     * 验证参数合法性
     */
    public void validate() {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        if (minLongitude > maxLongitude || minLatitude > maxLatitude) {
            throw new IllegalArgumentException("区域坐标设置错误");
        }

        // 网格大小合理性检查
        double lonDiff = maxLongitude - minLongitude;
        double latDiff = maxLatitude - minLatitude;
        double lonGridSize = gridSize / 85.3; // 北京地区1度经度约等于85.3公里
        double latGridSize = gridSize / 111.0; // 北京地区1度纬度约等于111公里

        if (lonDiff / lonGridSize > 100 || latDiff / latGridSize > 100) {
            throw new IllegalArgumentException("网格划分过细，请增大网格大小或减小区域范围");
        }
    }

    public DensityQuery() {
    }

    public DensityQuery(Double gridSize, LocalDateTime startTime, LocalDateTime endTime, Integer timeSlotMinutes) {
        this.gridSize = gridSize;
        this.startTime = startTime;
        this.endTime = endTime;
        if (timeSlotMinutes != null) {
            this.timeSlotMinutes = timeSlotMinutes;
        }
    }
}
