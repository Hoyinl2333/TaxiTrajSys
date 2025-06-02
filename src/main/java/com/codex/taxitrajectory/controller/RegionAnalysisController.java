package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.result.RegionQueryResult;
import com.codex.taxitrajectory.service.RegionQueryService;
import jakarta.servlet.http.HttpServletRequest; // 导入 HttpServletRequest
import jakarta.validation.Valid; // 导入 @Valid
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus; // 导入 HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // 导入 PostMapping

import java.util.UUID;

@RestController
@RequestMapping("/region")
public class RegionAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(RegionAnalysisController.class);
    private final RegionQueryService regionQueryService;

    public RegionAnalysisController(RegionQueryService regionQueryService) {
        this.regionQueryService = regionQueryService;
    }

    /**
     * F3: 区域范围查找。
     * 接收一个包含地理边界和时间范围的RegionQuery对象。
     * @param query 区域查询参数对象，通过 @Valid 进行校验。
     * @param request HTTP请求对象。
     * @return 包含区域内出租车数量和GPS点列表的ResponseEntity。
     */
    @PostMapping("/count")
    public ResponseEntity<RegionQueryResult> countTaxisInRegion(
            @RequestBody @Valid RegionQuery query, HttpServletRequest request) { @Valid

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTimeMillis = System.currentTimeMillis();

        logger.info("==================== 请求处理开始 : API=[区域范围查找F3] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());


        RegionQueryResult result = regionQueryService.getTaxisInRegion(query);

        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        logger.info("响应结果 : ID=[{}], 状态码=[{}], 处理耗时=[{}ms]", requestId, HttpStatus.OK.value(), durationMillis);
        if (result != null) {
            logger.info("响应内容摘要 : ID=[{}], 出租车数量=[{}]", requestId, result.getTaxiCount());
        }
        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(result);
    }
}