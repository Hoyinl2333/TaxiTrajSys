package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionCorrelationQuery;
import com.codex.taxitrajectory.model.query.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.service.UnifiedRegionCorrelationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/correlation")
public class UnifiedRegionCorrelationController {

    private final UnifiedRegionCorrelationService unifiedRegionCorrelationService;

    public UnifiedRegionCorrelationController(UnifiedRegionCorrelationService unifiedRegionCorrelationService) {
        this.unifiedRegionCorrelationService = unifiedRegionCorrelationService;
    }

    @PostMapping("/trafficFlowChangeBetweenRegions")
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeBetweenRegions(@RequestBody RegionCorrelationQuery query) {
        return unifiedRegionCorrelationService.analyzeTrafficFlowChangeBetweenRegions(query);
    }

    @PostMapping("/trafficFlowChangeWithOtherRegions")
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeWithOtherRegions(@RequestBody RegionSingleCorrelationQuery query) {
        return unifiedRegionCorrelationService.analyzeTrafficFlowChangeWithOtherRegions(query);
    }
}