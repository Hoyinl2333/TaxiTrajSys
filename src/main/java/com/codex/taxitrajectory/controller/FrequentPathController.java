package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.service.FrequentPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paths") // 所有路径相关的API都以 /paths 开头
public class FrequentPathController {

    private final FrequentPathService frequentPathService;

    @Autowired
    public FrequentPathController(FrequentPathService frequentPathService) {
        this.frequentPathService = frequentPathService;
    }

    /**
     * F7: 获取全市 Top-K 频繁路径
     * 通过 POST 请求接收 JSON 对象，其中包含 k (路径数量) 和 minPathDistanceKM (最小路径距离，单位千米)。
     *
     * @param query 包含查询参数 (k, minPathDistanceKM) 的 FrequentPathQuery 对象，从请求体中获取。
     * 确保 FrequentPathQuery 类有 k 和 minPathDistanceKM 字段，
     * 以及无参构造函数和对应的 getters/setters。
     * @return ResponseEntity 包含 FrequentPathResult 的响应实体。
     * 如果查询参数无效，则返回 400 Bad Request。
     * 如果成功，则返回 200 OK 及分析结果。
     */
    @PostMapping("/frequent/citywide")
    public ResponseEntity<FrequentPathResult> getCitywideFrequentPaths(
            @RequestBody FrequentPathQuery query
    ) {
        //  query 对象的字段进行校验。
        if (query == null || !query.isValid()) {
            // log.warn("无效的频繁路径查询参数: {}", query); // 考虑添加日志
            return ResponseEntity.badRequest().build(); // 构建一个更详细的错误响应 DTO 会更好
        }

        // 调用服务层进行频繁路径分析
        FrequentPathResult result = frequentPathService.analyzeFrequentPaths(query);
        return ResponseEntity.ok(result);
    }

    /**
     * F8: 获取区域 A 到区域 B 的 Top-K 频繁路径
     * 通过 POST 请求接收 JSON 对象，其中包含 k, minPathDistanceKM (可选，或由后端设定), regionA, regionB。
     *
     * @param query 包含查询参数 (k, minPathDistanceKM, regionA, regionB) 的 FrequentPathQuery 对象。
     * @return ResponseEntity 包含 FrequentPathResult 的响应实体。
     */
    @PostMapping("/frequent/regional")
    public ResponseEntity<FrequentPathResult> getRegionalFrequentPaths(
            @RequestBody FrequentPathQuery query // 接收包含 k, minDistance, regionA, regionB 的对象
    ) {


        // 校验 F8 特定条件，例如 regionA 和 regionB 是否存在且有效
        if (query == null || !query.isRegionQuery() || !query.isValid()) {
            return ResponseEntity.badRequest().build(); // 构建一个更详细的错误响应 DTO 会更好
        }

        FrequentPathResult result = frequentPathService.analyzeFrequentPaths(query);
        return ResponseEntity.ok(result);
    }
}