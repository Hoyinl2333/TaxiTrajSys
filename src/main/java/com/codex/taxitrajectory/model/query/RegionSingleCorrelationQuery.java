package com.codex.taxitrajectory.model.query;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegionSingleCorrelationQuery {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int timeSlotMinutes;
    private double topLeftLongitude;
    private double topLeftLatitude;
    private double bottomRightLongitude;
    private double bottomRightLatitude;

    public void validate() {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        if (topLeftLongitude > bottomRightLongitude || topLeftLatitude < bottomRightLatitude) {
            throw new IllegalArgumentException("区域坐标设置错误");
        }
    }
}