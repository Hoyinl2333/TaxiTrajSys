package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.service.RegionQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/region")
public class RegionAnalysisController {

    private final RegionQueryService regionQueryService;

    public RegionAnalysisController(RegionQueryService regionQueryService) {
        this.regionQueryService = regionQueryService;
    }

    /**
     * F3: 区域范围查找
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> countTaxisInRegion(
            @RequestParam double lon1,
            @RequestParam double lat1,
            @RequestParam double lon2,
            @RequestParam double lat2,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        // 创建Query对象
        RegionQuery query = new RegionQuery(
                Math.min(lon1, lon2),
                Math.min(lat1, lat2),
                Math.max(lon1, lon2),
                Math.max(lat1, lat2),
                startTime,
                endTime
        );

        query.validate();
        int taxiCount = regionQueryService.getTaxisInRegion(query).getTaxiCount();
        return ResponseEntity.ok(taxiCount);
    }
}