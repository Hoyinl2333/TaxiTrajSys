package com.codex.taxitrajectory.model.query;

import lombok.Data;

import java.time.LocalDateTime;

// 定义一个公共的外部类
public class RegionQueryWrapper {

    // 内部类 RegionSingleCorrelationQuery
    @Data
    public static class RegionSingleCorrelationQuery {
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

    // 内部类 RegionCorrelationQuery
    @Data
    public static class RegionCorrelationQuery {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int timeSlotMinutes;
        private double topLeftLongitude1;
        private double topLeftLatitude1;
        private double bottomRightLongitude1;
        private double bottomRightLatitude1;
        private double topLeftLongitude2;
        private double topLeftLatitude2;
        private double bottomRightLongitude2;
        private double bottomRightLatitude2;
    }
}