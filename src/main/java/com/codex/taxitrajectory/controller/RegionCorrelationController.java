package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionCorrelationQuery;
import com.codex.taxitrajectory.service.RegionCorrelationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegionCorrelationController {

    private final RegionCorrelationService regionCorrelationService;

    public RegionCorrelationController(RegionCorrelationService regionCorrelationService) {
        this.regionCorrelationService = regionCorrelationService;
    }

    /**
     * 分析不同时间段内两个指定区域间的车流量变化
     */
    @PostMapping("/trafficFlowChangeBetweenRegions")
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeBetweenRegions(@RequestBody RegionCorrelationQuery query) {
        return regionCorrelationService.analyzeTrafficFlowChangeBetweenRegions(query);
    }
}