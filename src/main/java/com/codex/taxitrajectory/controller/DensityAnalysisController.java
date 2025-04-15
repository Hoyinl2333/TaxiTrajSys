package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/density")
public class DensityAnalysisController {

    private final DensityAnalysisService densityAnalysisService;

    public DensityAnalysisController(DensityAnalysisService densityAnalysisService) {
        this.densityAnalysisService = densityAnalysisService;
    }

    /**
     * F4: 区域车流密度分析 - 使用 Query 类作为请求体
     * POST 方法接收 JSON 格式的请求体
     */
    @PostMapping("/analyze")
    public DensityAnalysisResult analyzeTrafficDensity(@RequestBody @Valid DensityQuery query) {
        query.validate();
        return densityAnalysisService.analyzeTrafficDensity(query);
    }

    /**
     * F4: 区域车流密度分析 - 同时支持 URL 参数方式（向后兼容）
     * GET 方法接收 URL 参数
     */
    @GetMapping("/analyze")
    public DensityAnalysisResult analyzeTrafficDensity(
            @RequestParam double gridSize,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "60") Integer timeSlotMinutes,
            @RequestParam(required = false) Double minLongitude,
            @RequestParam(required = false) Double minLatitude,
            @RequestParam(required = false) Double maxLongitude,
            @RequestParam(required = false) Double maxLatitude) {

        // 创建 DensityQuery 对象
        DensityQuery query = new DensityQuery(gridSize, startTime, endTime, timeSlotMinutes);
        if (minLongitude != null) {
            query.setMinLongitude(minLongitude);
        }
        if (minLatitude != null) {
            query.setMinLatitude(minLatitude);
        }
        if (maxLongitude != null) {
            query.setMaxLongitude(maxLongitude);
        }
        if (maxLatitude != null) {
            query.setMaxLatitude(maxLatitude);
        }
        query.validate();
        return densityAnalysisService.analyzeTrafficDensity(query);
    }
}
