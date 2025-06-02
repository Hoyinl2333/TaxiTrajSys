package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.result.DensityAnalysisResult;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.service.DensityAnalysisService;
import jakarta.servlet.http.HttpServletRequest; // 导入 HttpServletRequest
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus; // 导入 HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID; // 导入 UUID

@RestController
@RequestMapping("/densityAnalysis")
public class DensityAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(DensityAnalysisController.class);
    private final DensityAnalysisService densityAnalysisService;

    public DensityAnalysisController(DensityAnalysisService densityAnalysisService) {
        this.densityAnalysisService = densityAnalysisService;
    }

    /**
     * F4: 区域车流密度分析 - POST 方法接收 JSON 格式的请求体。
     * 请求体通过 @Valid 注解进行校验，校验失败将由 GlobalExceptionHandler 处理。
     * @param query 包含密度分析所需参数的查询对象。
     * @param request HTTP请求对象，用于获取请求信息。
     * @return 成功时返回包含分析结果的 ResponseEntity。
     */
    @PostMapping("/analyze")
    public ResponseEntity<DensityAnalysisResult> analyzeTrafficDensity(
            @RequestBody @Valid DensityQuery query, HttpServletRequest request) {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[区域车流密度分析] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());


        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }
}