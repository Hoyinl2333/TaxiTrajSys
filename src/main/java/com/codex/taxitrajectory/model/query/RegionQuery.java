package com.codex.taxitrajectory.model.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 区域范围查询参数类
 * 用于F3功能：区域范围查找
 */
@Data
public class RegionQuery {
    @NotNull(message = "左上角经度不能为空")
    private Double minLongitude; // 最小经度

    @NotNull(message = "左上角纬度不能为空")
    private Double minLatitude;  // 最小纬度

    @NotNull(message = "右下角经度不能为空")
    private Double maxLongitude; // 最大经度

    @NotNull(message = "右下角纬度不能为空")
    private Double maxLatitude;  // 最大纬度

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime; // 开始时间

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;   // 结束时间

    // 构造函数、getter和setter省略
    public RegionQuery() {
    }

    public RegionQuery(Double minLongitude, Double minLatitude, Double maxLongitude,
                       Double maxLatitude, LocalDateTime startTime, LocalDateTime endTime) {
        this.minLongitude = minLongitude;
        this.minLatitude = minLatitude;
        this.maxLongitude = maxLongitude;
        this.maxLatitude = maxLatitude;
        this.startTime = startTime;
        this.endTime = endTime;
    }




    // 数据验证方法
    public void validate() {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        if (minLongitude > maxLongitude || minLatitude > maxLatitude) {
            throw new IllegalArgumentException("区域坐标设置错误");
        }
    }
}
