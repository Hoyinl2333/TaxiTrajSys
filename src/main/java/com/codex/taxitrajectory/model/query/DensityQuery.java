package com.codex.taxitrajectory.model.query;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DensityQuery {
    @Positive(message = "网格大小必须为正数")
    @NotNull(message = "网格大小不能为空") // 如果gridSize也是强制的
    private Double gridSize;  // 网格大小(单位:km)

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime; // 开始时间

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;   // 结束时间

    @Positive(message = "时间间隔必须为正数")
    @NotNull(message = "时间间隔不能为空") // 如果时间间隔也是强制的
    private Integer timeSlotMinutes = 60; // 时间分割粒度，默认为60分钟

    // 新增：强制性的地理边界参数
    @NotNull(message = "最小经度不能为空")
    private Double minLongitude;

    @NotNull(message = "最小纬度不能为空")
    private Double minLatitude;

    @NotNull(message = "最大经度不能为空")
    private Double maxLongitude;

    @NotNull(message = "最大纬度不能为空")
    private Double maxLatitude;

    // 构造函数
    public DensityQuery() {
    }

    // 构造函数
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

    public void validate() {
        if (startTime == null || endTime == null || gridSize == null || timeSlotMinutes == null ||
                minLongitude == null || minLatitude == null || maxLongitude == null || maxLatitude == null) {
            throw new IllegalArgumentException("所有查询参数（包括地理边界）均不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        if (gridSize <= 0) {
            throw new IllegalArgumentException("网格大小必须为正数");
        }
        if (timeSlotMinutes <= 0) {
            throw new IllegalArgumentException("时间间隔必须为正数");
        }
        if (minLongitude >= maxLongitude) {
            throw new IllegalArgumentException("最小经度必须小于最大经度");
        }
        if (minLatitude >= maxLatitude) {
            throw new IllegalArgumentException("最小纬度必须小于最大纬度");
        }
         if (minLongitude < -180 || maxLongitude > 180 || minLatitude < -90 || maxLatitude > 90) {
             throw new IllegalArgumentException("经纬度值超出有效范围");
         }
    }
}