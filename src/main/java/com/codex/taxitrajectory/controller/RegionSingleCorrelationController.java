package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.service.RegionSingleCorrelationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/SingleCorrelation")
public class RegionSingleCorrelationController {

    private final RegionSingleCorrelationService regionSingleCorrelationService;

    public RegionSingleCorrelationController(RegionSingleCorrelationService regionSingleCorrelationService) {
        this.regionSingleCorrelationService = regionSingleCorrelationService;
    }

    @PostMapping("/trafficFlowChangeWithOtherRegions")
    public Map<LocalDateTime, int[]> analyzeTrafficFlowChangeWithOtherRegions(@RequestBody RegionSingleCorrelationQuery query) {
        return regionSingleCorrelationService.analyzeTrafficFlowChangeWithOtherRegions(query);
    }
}