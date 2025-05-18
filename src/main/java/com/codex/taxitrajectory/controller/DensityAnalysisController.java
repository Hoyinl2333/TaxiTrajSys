package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.result.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/densityAnalysis")
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
    @GetMapping("/densityAnalysis")
    public ResponseEntity<DensityAnalysisResult> analyzeTrafficDensity(
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
        query.validate();

        return ResponseEntity.ok(densityAnalysisService.analyzeTrafficDensity(query));
    }
}
