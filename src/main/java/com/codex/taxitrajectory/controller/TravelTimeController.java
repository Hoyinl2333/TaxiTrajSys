package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.TravelTimeQuery;
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.service.TravelTimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID; // 导入 UUID
import java.util.concurrent.TimeUnit; // 导入 TimeUnit

@RestController
@RequestMapping("/travelTime")
public class TravelTimeController {

    private static final Logger logger = LoggerFactory.getLogger(TravelTimeController.class);

    private final TravelTimeService travelTimeService;

    @Autowired
    public TravelTimeController(TravelTimeService travelTimeService) {
        this.travelTimeService = travelTimeService;
    }

    /**
     * F9 通行时间分析 API 端点。
     * 接收包含区域和单一时间段的查询参数，执行分析并返回结果。
     *
     * @param query 包含区域和单一时间段的查询对象
     * @param request HTTP请求对象
     * @return ResponseEntity 包含分析结果 TravelTimeResult 或由全局异常处理器处理的错误信息
     */
    @PostMapping("/analyze")
    public ResponseEntity<TravelTimeResult> analyzeTravelTime(
                                                               @RequestBody @Valid TravelTimeQuery query,
                                                               HttpServletRequest request) {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long processingStartTimeNanos = System.nanoTime();


        logger.info("==================== 请求处理开始 : API=[通行时间分析F9] ===================="); //
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId); //
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString()); //

        TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);

        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processingStartTimeNanos);

        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        if (result != null && result.isFound()) {
            logger.info("响应内容摘要 : ID=[{}], 找到路径, 通行时间=[{}]", requestId, result.getMinTravelTimeFormatted());
        } else {
            logger.info("响应内容摘要 : ID=[{}], 未找到路径或结果为空", requestId);
        }
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }
}