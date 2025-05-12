package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.TaxiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/taxi")
public class TaxiController {

    private static final Logger logger = LoggerFactory.getLogger(TaxiController.class);

    private final TaxiRepository taxiRepository;


    public TaxiController(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<TaxiRecord>> getTaxiTrajectory(@PathVariable String id) {

        logger.info("============================");
        logger.info("收到出租车轨迹绘制请求. Taxi ID: {}", id);
        long startTime = System.nanoTime();

        List<TaxiRecord> records;
        try {
            records = taxiRepository.getRecordsByTaxiIdAsList(id);
        } catch (Exception e) {
            logger.error("获取数据发生异常！ Taxi ID: {}. Error: {}", id, e.getMessage(), e);
            // 根据您的错误处理策略，这里可以向上抛出自定义异常或直接返回错误响应
            // 例如: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching data");
            throw e;
        }

        // 计算耗时
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // 记录获取到的数据量和耗时
        if (records != null) {
            logger.info("成功获取出租车ID: {} 的轨迹数据共 {} 条，耗时: {} 毫秒", id, records.size(), durationMs);
        } else {
            logger.warn("未能获取出租车ID: {} 的轨迹数据（返回为null），耗时: {} 毫秒。。", id, durationMs);
        }
        logger.info("============================");

        return ResponseEntity.ok(records);
    }
}