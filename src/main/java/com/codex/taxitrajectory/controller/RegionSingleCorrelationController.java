package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.service.RegionSingleCorrelationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegionSingleCorrelationController {

    private final RegionSingleCorrelationService regionSingleCorrelationService;

    public RegionSingleCorrelationController(RegionSingleCorrelationService regionSingleCorrelationService) {
        this.regionSingleCorrelationService = regionSingleCorrelationService;
    }

    /**
     * 分析指定矩形区域与其他区域的车流量随时间的变化
     */
    @PostMapping("/trafficFlowChangeWithOtherRegions")
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeWithOtherRegions(@RequestBody RegionSingleCorrelationQuery query) {
        return regionSingleCorrelationService.analyzeTrafficFlowChangeWithOtherRegions(query);
    }
}