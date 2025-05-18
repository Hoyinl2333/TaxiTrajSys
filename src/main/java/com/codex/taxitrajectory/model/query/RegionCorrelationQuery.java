package com.codex.taxitrajectory.model.query;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegionCorrelationQuery {
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