package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.TaxiRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/taxi")
@Validated
public class TaxiController {

    private static final Logger logger = LoggerFactory.getLogger(TaxiController.class);
    private final TaxiRepository taxiRepository;

    public TaxiController(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    /**
     * F1: 根据出租车ID获取其轨迹数据。
     * @param id 出租车ID，通过路径变量传递，必须为正整数。
     * @param request HTTP请求对象。
     * @return 包含出租车轨迹记录列表的ResponseEntity。
     */
    @GetMapping("/{id}")
    public ResponseEntity<List<TaxiRecord>> getTaxiTrajectory(
            @PathVariable @NotNull(message = "出租车ID不能为空")
            @Min(value = 1, message = "出租车ID必须是大于0的数字")
            @Max(value = 10357,message = "出租车ID必须是小于10358的数字")
            Long id, // 将类型从 String 修改为 Long
            HttpServletRequest request) {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long processingStartTimeNanos = System.nanoTime();

        logger.info("==================== 请求处理开始 : API=[获取出租车轨迹F1] ====================");
        logger.info("接收请求 : 方法=[{}], 路径=[{}], 请求ID=[{}]", request.getMethod(), request.getRequestURI(), requestId);
        logger.info("请求参数 : ID=[{}], 出租车ID=[{}]", requestId, id);


        List<TaxiRecord> records;
        try {
            // TaxiRepository 通常期望的是字符串ID，所以这里需要将 Long 转换回 String
            records = taxiRepository.getRecordsByTaxiIdAsList(String.valueOf(id));
        } catch (Exception e) {
            logger.error("获取出租车轨迹数据时发生异常 : ID=[{}], 出租车ID=[{}], 错误=[{}]",
                    requestId, id, e.getMessage(), e);
            throw e; // 重新抛出，让GlobalExceptionHandler处理
        }

        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processingStartTimeNanos);

        logger.info("响应结果 : ID=[{}], 状态码=[{}], 记录数=[{}], 处理耗时=[{}ms]",
                requestId, HttpStatus.OK.value(), (records != null ? records.size() : 0), durationMillis);

        logger.info("==================== 请求处理结束 : ID=[{}] ====================", requestId);

        return ResponseEntity.ok(records);
    }
}