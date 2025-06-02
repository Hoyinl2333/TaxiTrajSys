package com.codex.taxitrajectory.model.result;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RegionCorrelationResult {
    private Map<LocalDateTime, int[]> trafficFlowChange;
}