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
            @RequestBody @Valid DensityQuery query, HttpServletRequest request) { // 添加 HttpServletRequest request

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[区域车流密度分析] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        // 注意：直接打印整个 query 对象可能包含大量数据。
        // 您可以考虑自定义 query.toString() 或只记录部分关键字段，例如：
        // logger.info("请求参数 : ID=[{}], 时间范围=[{}至{}], 网格大小=[{}]", requestId, query.getStartTime(), query.getEndTime(), query.getGridSize());
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());


        DensityAnalysisResult result = densityAnalysisService.analyzeTrafficDensity(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        // 如果需要记录响应体摘要，可以添加，但同样注意数据量
        // logger.info("响应内容摘要 : ID=[{}], 网格行列=[{}x{}], 时间槽数=[{}]", requestId, result.getRows(), result.getCols(), result.getTimeSlots() != null ? result.getTimeSlots().size() : 0);
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }
}