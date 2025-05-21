package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionQueryWrapper.RegionCorrelationQuery;
import com.codex.taxitrajectory.model.query.RegionQueryWrapper.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.model.result.RegionCorrelationResult;
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
    public RegionCorrelationResult analyzeTrafficFlowChangeBetweenRegions(@RequestBody RegionCorrelationQuery query) {
        return unifiedRegionCorrelationService.analyzeTrafficFlowChangeBetweenRegions(query);
    }

    @PostMapping("/trafficFlowChangeWithOtherRegions")
    public RegionCorrelationResult analyzeTrafficFlowChangeWithOtherRegions(@RequestBody RegionSingleCorrelationQuery query) {
        return unifiedRegionCorrelationService.analyzeTrafficFlowChangeWithOtherRegions(query);
    }
}