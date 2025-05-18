package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.service.RegionQueryService;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class RegionAnalysisController {

    private final RegionQueryService regionQueryService;
    private final DensityAnalysisService densityAnalysisService;

    public RegionAnalysisController(RegionQueryService regionQueryService,
                                    DensityAnalysisService densityAnalysisService) {
        this.regionQueryService = regionQueryService;
        this.densityAnalysisService = densityAnalysisService;
    }

    /**
     * F3: 区域范围查找 - 使用Query类作为请求体
     */
    @PostMapping("/region/count")
    public int countTaxisInRegion(@RequestBody @Valid RegionQuery query) {
        query.validate();
        return regionQueryService.countTaxisInRegion(query);
    }

    /**
     * F3: 区域范围查找 - 同时支持URL参数方式(向后兼容)
     */
    @GetMapping("/region/count")
    public int countTaxisInRegion(
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
        return regionQueryService.countTaxisInRegion(query);
    }

    /**
     * F4: 区域车流密度分析 - 使用Query类作为请求体
     * POST方法接收JSON格式的请求体
     */
    @PostMapping("/density/analyze")
    public DensityAnalysisResult analyzeTrafficDensity(@RequestBody @Valid DensityQuery query) {
        query.validate();
        return densityAnalysisService.analyzeTrafficDensity(query);
    }

    /**
     * F4: 区域车流密度分析 - 同时支持URL参数方式(向后兼容)
     * GET方法接收URL参数
     */
    @GetMapping("/density/analyze")
    public DensityAnalysisResult analyzeTrafficDensity(
            @RequestParam double gridSize,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "60") Integer timeSlotMinutes,
            @RequestParam(required = false) Double minLongitude,
            @RequestParam(required = false) Double minLatitude,
            @RequestParam(required = false) Double maxLongitude,
            @RequestParam(required = false) Double maxLatitude) {

        // 创建DensityQuery对象
        DensityQuery query = new DensityQuery(gridSize, startTime, endTime, timeSlotMinutes);

        // 设置可选的区域范围参数
        if (minLongitude != null) query.setMinLongitude(minLongitude);
        if (minLatitude != null) query.setMinLatitude(minLatitude);
        if (maxLongitude != null) query.setMaxLongitude(maxLongitude);
        if (maxLatitude != null) query.setMaxLatitude(maxLatitude);

        query.validate();
        return densityAnalysisService.analyzeTrafficDensity(query);
    }
}
