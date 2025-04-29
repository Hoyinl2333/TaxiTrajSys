package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.TravelTimeQuery;
import com.codex.taxitrajectory.model.result.TravelTimeResult;
import com.codex.taxitrajectory.service.TravelTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // 用于返回 HTTP 响应
import org.springframework.web.bind.annotation.*; // 引入 Spring Web 注解

import java.util.Collections;


@RestController // 声明这是一个 REST 控制器
@RequestMapping("/travelTime") // 定义基础路径
public class TravelTimeController {

    private static final Logger log = LoggerFactory.getLogger(TravelTimeController.class);

    private final TravelTimeService travelTimeService;

    @Autowired
    public TravelTimeController(TravelTimeService travelTimeService) {
        this.travelTimeService = travelTimeService;
    }

    /**
     * F9 通行时间分析 API 端点。
     * 接收包含区域和时间段的查询参数，执行分析并返回结果。
     *
     * 示例请求体 (POST /api/travel-time/analyze):
     * {
     *   "regionA": { "minLon": 116.45, "minLat": 40.00, "maxLon": 116.50, "maxLat": 40.05 },
     *   "regionB": { "minLon": 116.44, "minLat": 39.90, "maxLon": 116.48, "maxLat": 39.92 },
     *   "timeIntervals": [
     *     { "startTime": "2008-02-03T07:00:00", "endTime": "2008-02-03T09:00:00" },
     *     { "startTime": "2008-02-03T17:00:00", "endTime": "2008-02-03T19:00:00" }
     *   ]
     * }
     *
     * @param query 包含区域和时间段的查询对象 (通过 @RequestBody 从请求体 JSON 自动映射)
     * @return ResponseEntity 包含分析结果 TravelTimeResult 或错误信息
     */
    @PostMapping("/analyze") // 使用 POST 方法，因为请求体可能较大或包含敏感坐标
    public ResponseEntity<?> analyzeTravelTime(@RequestBody TravelTimeQuery query) {
        log.info("收到通行时间分析请求: {}", query);

        // 1. 基本的输入检查 (更详细的验证在 Service 层)
        if (query == null || query.getRegionA() == null || query.getRegionB() == null ||
                query.getTimeIntervals() == null || query.getTimeIntervals().isEmpty()) {
            log.warn("收到的查询参数无效: {}", query);
            return ResponseEntity.badRequest().body("无效的查询参数：区域 A、区域 B 和时间段列表不能为空。"); // 返回 400 Bad Request
        }

        try {
            // 2. 调用 Service 层执行分析
            TravelTimeResult result = travelTimeService.analyzeShortestTravelTime(query);

            // 3. 处理并返回结果
            if (result != null && result.getResults() != null) {
                // 可以在这里对结果进行一些处理或转换，如果需要的话
                // 例如，如果前端不需要完整的 TaxiRecord 列表，可以只提取关键信息

                // 移除包含完整轨迹的结果，以减小响应大小（如果前端不需要）
                // 如果前端需要绘制路径，则不应移除
                /*
                if (result.getResults() != null) {
                    result.getResults().values().forEach(info -> {
                        if (info != null) {
                            // info.setShortestPath(null); // 清空路径信息，只保留时间和 found 状态
                        }
                    });
                }
                */

                log.info("通行时间分析成功完成，返回结果。");
                return ResponseEntity.ok(result); // 返回 200 OK 和结果 JSON
            } else {
                // Service 层返回了 null 或空结果 (可能由于验证失败或无数据)
                log.warn("通行时间分析服务返回空结果。");
                // 可以根据 Service 层返回的具体情况返回不同的状态码，这里简单返回空内容
                return ResponseEntity.ok(new TravelTimeResult(Collections.emptyMap())); // 返回 200 OK 和空结果集
            }

        } catch (IllegalArgumentException e) {
            // 捕获 Service 层明确抛出的参数错误
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