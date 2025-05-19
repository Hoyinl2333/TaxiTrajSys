package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.result.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/densityAnalysis")
public class DensityAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(DensityAnalysisController.class);
    private final DensityAnalysisService densityAnalysisService;

    public DensityAnalysisController(DensityAnalysisService densityAnalysisService) {
        this.densityAnalysisService = densityAnalysisService;
    }

    /**
     * F4: 区域车流密度分析 - POST 方法接收 JSON 格式的请求体 (推荐)
     * 请求体应包含 gridSize, startTime, endTime, timeSlotMinutes,
     * minLongitude, minLatitude, maxLongitude, maxLatitude.
     */
    @PostMapping("/analyze")
    public ResponseEntity<DensityAnalysisResult> analyzeTrafficDensity(@RequestBody @Valid DensityQuery query) {
        logger.info("接收到车流密度分析POST请求: {}", query);
        try {
            // DensityQuery 内部的 @NotNull 等注解会由 @Valid 触发校验
            // 自定义的 validate() 方法也需要在服务层或此处显式调用 (服务层已调用)
            DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);
            logger.info("车流密度分析POST请求处理成功. 查询参数: {}", query);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.warn("车流密度分析POST请求参数无效. 查询: {},错误: {}", query, e.getMessage());
            throw e; // Spring Boot 会转换为 400 Bad Request
        } catch (Exception e) {
            logger.error("车流密度分析POST请求处理时发生内部错误. 查询: {}", query, e);
            // 可以返回一个更通用的错误响应对象或状态码
            throw e; // Spring Boot 会转换为 500 Internal Server Error
        }
    }
//
//    /**
//     * F4: 区域车流密度分析 - GET 方法接收 URL 参数 (向后兼容)
//     * 注意：此GET方法可能难以满足所有强制参数（特别是新加入的地理边界）的要求，
//     * 除非在 DensityQuery 中为这些新增的边界参数提供合理的 @RequestParam(required=true) 映射。
//     * 推荐使用POST /analyze接口。
//     */
//    @GetMapping("/densityAnalysis")
//    public ResponseEntity<DensityAnalysisResult> analyzeTrafficDensityFromGet(
//            @RequestParam @NotNull Double gridSize, // 确保所有参数都从URL获取
//            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
//            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
//            @RequestParam(required = false, defaultValue = "60") Integer timeSlotMinutes,
//            @RequestParam @NotNull Double minLongitude, // 新增，需要前端在GET请求中提供
//            @RequestParam @NotNull Double minLatitude,   // 新增
//            @RequestParam @NotNull Double maxLongitude,  // 新增
//            @RequestParam @NotNull Double maxLatitude    // 新增
//    ) {
//
//        logger.info("接收到车流密度分析GET请求: gridSize={}, startTime={}, endTime={}, timeSlotMinutes={}, minLon={}, minLat={}, maxLon={}, maxLat={}",
//                gridSize, startTime, endTime, timeSlotMinutes, minLongitude, minLatitude, maxLongitude, maxLatitude);
//
//        // 手动创建 DensityQuery 对象
//        DensityQuery query = new DensityQuery(gridSize, startTime, endTime, timeSlotMinutes,
//                minLongitude, minLatitude, maxLongitude, maxLatitude);
//        // query.validate(); // 已在服务层调用，或者如果希望在controller层提前校验也可以
//
//        try {
//            DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);
//            logger.info("车流密度分析GET请求处理成功.");
//            return ResponseEntity.ok(result);
//        } catch (IllegalArgumentException e) {
//            logger.warn("车流密度分析GET请求参数无效. 错误: {}", e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            logger.error("车流密度分析GET请求处理时发生内部错误.", e);
//            throw e;
//        }
//    }
}