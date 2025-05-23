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
        // 考虑 FrequentPathQuery 的 toString() 是否合适，或者记录关键字段
        logger.info("请求参数 : ID=[{}], 参数=[{}]", requestId, query.toString());

        // 移除了手动校验 (query == null || !query.isValid())
        // @Valid 会处理DTO的校验，失败则由GlobalExceptionHandler捕获

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

        // 移除了手动校验 (query == null || !query.isRegionQuery() || !query.isValid())
        // @Valid 会处理DTO的校验。
        // 对于 !query.isRegionQuery() 这种业务逻辑（即F8查询必须提供regionA和regionB），
        // 如果 FrequentPathQuery 的 @Valid 不能完全覆盖（例如，regionA和regionB本身是@NotNull，但需要它们同时存在），
        // 那么这个检查应该在 Service 层进行，如果校验失败则抛出 IllegalArgumentException。
        // 或者，为 FrequentPathQuery 创建一个更复杂的类级别校验注解，专门用于F8场景，使用校验组 (groups)。
        // 目前，我们假设 @Valid 结合 FrequentPathQuery 内部的注解（包括 @Valid on Region）已足够。
        // 如果 Service 层发现是F8场景但区域信息不完整，它应该抛出异常。
        if (!query.isRegionQuery()) {
            // 这种情况表明客户端可能错误地调用了regional接口却没有提供区域参数
            // 或者DTO设计上允许regionA/B为null，但此接口业务上不允许。
            // GlobalExceptionHandler会捕获此IllegalArgumentException。
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