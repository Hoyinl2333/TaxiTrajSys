package com.codex.taxitrajectory.controller;

// 确保导入的是重命名和修改后的查询类
import com.codex.taxitrajectory.model.query.CorrelationQuery.RegionCorrelationQuery;
import com.codex.taxitrajectory.model.query.CorrelationQuery.RegionSingleCorrelationQuery;
import com.codex.taxitrajectory.model.result.CorrelationResult;
import com.codex.taxitrajectory.service.CorrelationService;
import jakarta.servlet.http.HttpServletRequest; // 导入 HttpServletRequest
import jakarta.validation.Valid; // 导入 @Valid
import org.slf4j.Logger; // 导入 Logger
import org.slf4j.LoggerFactory; // 导入 LoggerFactory
import org.springframework.http.HttpStatus; // 导入 HttpStatus
import org.springframework.http.ResponseEntity; // 导入 ResponseEntity
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID; // 导入 UUID

@RestController
@RequestMapping("/correlation")
public class CorrelationController {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationController.class); // 添加 logger
    private final CorrelationService unifiedRegionCorrelationService;

    public CorrelationController(CorrelationService correlationService) {
        this.unifiedRegionCorrelationService = correlationService;
    }

    /**
     * F5: 分析两个指定区域间的车流量变化。
     * @param query 包含两个区域定义、时间范围和时间槽的查询对象。
     * @param request HTTP请求对象。
     * @return 包含分析结果的ResponseEntity。
     */
    @PostMapping("/trafficFlowChangeBetweenRegions")
    public ResponseEntity<CorrelationResult> analyzeTrafficFlowChangeBetweenRegions(
            @RequestBody @Valid RegionCorrelationQuery query, HttpServletRequest request) { // 添加 @Valid 和 HttpServletRequest

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[区域间关联分析F5] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        // CorrelationQuery.RegionCorrelationQuery DTO现在由 @Valid 自动校验
        // 包括其内部的 @Valid Region (如果按我们之前重构的那样) 和类级别的 @ValidTimeRange
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());

        CorrelationResult result = unifiedRegionCorrelationService.analyzeTrafficFlowChangeBetweenRegions(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        // 可以选择性记录结果摘要
        if (result != null && result.getTrafficFlowChange() != null) {
            logger.info("响应内容摘要 : ID=[{}], 时间槽数量=[{}]", requestId, result.getTrafficFlowChange().size());
        }
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }

    /**
     * F6: 分析指定矩形区域与其他区域的车流量随时间的变化。
     * @param query 包含单个目标区域、时间范围和时间槽的查询对象。
     * @param request HTTP请求对象。
     * @return 包含分析结果的ResponseEntity。
     */
    @PostMapping("/trafficFlowChangeWithOtherRegions")
    public ResponseEntity<CorrelationResult> analyzeTrafficFlowChangeWithOtherRegions(
            @RequestBody @Valid RegionSingleCorrelationQuery query, HttpServletRequest request) { // 添加 @Valid 和 HttpServletRequest

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[单区域关联分析F6] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        // CorrelationQuery.RegionSingleCorrelationQuery DTO现在由 @Valid 自动校验
        // 包括其内部的字段级注解和类级别的 @ValidTimeRange 与 @ValidGeoBoundingBox
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());

        CorrelationResult result = unifiedRegionCorrelationService.analyzeTrafficFlowChangeWithOtherRegions(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        if (result != null && result.getTrafficFlowChange() != null) {
            logger.info("响应内容摘要 : ID=[{}], 时间槽数量=[{}]", requestId, result.getTrafficFlowChange().size());
        }
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }
}