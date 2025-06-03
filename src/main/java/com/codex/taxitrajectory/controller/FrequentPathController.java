package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.service.FrequentPathService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/paths")
public class FrequentPathController {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathController.class); // 添加 logger
    private final FrequentPathService frequentPathService;

    @Autowired
    public FrequentPathController(FrequentPathService frequentPathService) {
        this.frequentPathService = frequentPathService;
    }

    /**
     * F7: 获取全市Top-K频繁路径。
     * @param query 包含k和minPathDistanceKM的查询对象。
     * @param request HTTP请求对象。
     * @return 包含频繁路径分析结果的ResponseEntity。
     */
    @PostMapping("/frequent/citywide")
    public ResponseEntity<FrequentPathResult> getCitywideFrequentPaths(
            @RequestBody @Valid FrequentPathQuery query, HttpServletRequest request) { // 添加 @Valid 和 HttpServletRequest

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[全市频繁路径分析F7] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);

        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());

        FrequentPathResult result = frequentPathService.analyzeFrequentPaths(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        // 可以选择性记录结果摘要，例如找到的路径数量
        if (result != null && result.getPathFrequencies() != null) {
            logger.info("响应内容摘要 : ID=[{}], 找到路径数=[{}]", requestId, result.getPathFrequencies().size());
        }
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }

    /**
     * F8: 获取区域A到区域B的Top-K频繁路径。
     * @param query 包含k, minPathDistanceKM, regionA, regionB的查询对象。
     * @param request HTTP请求对象。
     * @return 包含频繁路径分析结果的ResponseEntity。
     */
    @PostMapping("/frequent/regional")
    public ResponseEntity<FrequentPathResult> getRegionalFrequentPaths(
            @RequestBody @Valid FrequentPathQuery query, HttpServletRequest request) {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[区域间频繁路径分析F8] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());

        if (!query.isRegionQuery()) {
            throw new IllegalArgumentException("区域间频繁路径查询 (F8) 必须提供起始区域A和目标区域B。");
        }

        FrequentPathResult result = frequentPathService.analyzeFrequentPaths(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        if (result != null && result.getPathFrequencies() != null) {
            logger.info("响应内容摘要 : ID=[{}], 找到路径数=[{}]", requestId, result.getPathFrequencies().size());
        }
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }
}