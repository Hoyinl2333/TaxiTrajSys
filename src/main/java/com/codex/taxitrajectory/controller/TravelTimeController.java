package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.TravelTimeQuery;
// 导入你重命名后的 TravelTimeResult 类
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.service.TravelTimeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 注意：不再需要导入 Collections，因为不再处理 Map

@RestController
@RequestMapping("/travelTime")
public class TravelTimeController {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeController.class);

    private final TravelTimeService travelTimeService;

    @Autowired
    public TravelTimeController(TravelTimeService travelTimeService) {
        this.travelTimeService = travelTimeService;
    }

    /**
     * F9 通行时间分析 API 端点（针对单一时间段）。
     * 接收包含区域和单一时间段的查询参数，执行分析并返回结果。
     *
     * 示例请求体 (POST /travelTime/analyze):
     * {
     * "regionA": { "minLon": 116.45, "minLat": 40.00, "maxLon": 116.50, "maxLat": 40.05 },
     * "regionB": { "minLon": 116.44, "minLat": 39.90, "maxLon": 116.48, "maxLat": 39.92 },
     * "startTime": "2008-02-03T07:00:00",
     * "endTime": "2008-02-03T09:00:00"
     * }
     *
     * @param query 包含区域和单一时间段的查询对象 (通过 @RequestBody 从请求体 JSON 自动映射)
     * @return ResponseEntity 包含分析结果 TravelTimeResult 或错误信息
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeTravelTime(@RequestBody @Valid TravelTimeQuery query) {
        log.info("=============================");
        log.info("收到通行时间分析请求: {}", query);

        // 1. 基本的输入检查 (检查区域和时间段是否为 null)
        if (query == null || query.getRegionA() == null || query.getRegionB() == null ||
                query.getStartTime() == null || query.getEndTime() == null) {
            log.warn("收到的查询参数无效：缺少区域 A、区域 B、开始时间或结束时间。");
            return ResponseEntity.badRequest().body("无效的查询参数：区域 A、区域 B、开始时间和结束时间不能为空。"); // 返回 400 Bad Request
        }


        try {
            // 2. 调用 Service 层执行分析
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);

            if (result.isFound()) {
                log.info("通行时间分析成功完成，找到最短路径，耗时 {}。", result.getMinTravelTimeFormatted());
            } else {
                log.info("通行时间分析完成，未找到符合条件的最短路径。");
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // 捕获 Service 层明确抛出的参数错误 (例如：时间范围无效、区域无效等)
            log.error("处理通行时间分析请求时发生参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body("查询参数错误: " + e.getMessage()); // 返回 400 Bad Request
        } catch (Exception e) {
            // 捕获其他未预期的服务器内部错误
            log.error("处理通行时间分析请求时发生内部服务器错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("服务器内部错误，请稍后重试。"); // 返回 500 Internal Server Error
        }
    }
}