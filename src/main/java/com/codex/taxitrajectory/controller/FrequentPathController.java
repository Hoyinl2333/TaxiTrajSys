package com.codex.taxitrajectory.controller;


import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.service.FrequentPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/paths")
public class FrequentPathController {

    private final FrequentPathService frequentPathService;

    @Autowired
    public FrequentPathController(FrequentPathService frequentPathService) {
        this.frequentPathService = frequentPathService;
    }

    /**
     * F7: 获取全市 Top-K 频繁路径
     * 示例 URL: GET /api/paths/frequent/citywide?k=10&minDistance=1000
     * (minDistance 单位是米)
     */
    @GetMapping("/frequent/citywide")
    public ResponseEntity<FrequentPathResult> getCitywideFrequentPaths(
            @RequestParam(defaultValue = "10") int k,
            @RequestParam(defaultValue = "1000") double minPathDistanceMeters // 用户输入的距离 x (米)
    ) {
        FrequentPathQuery query = new FrequentPathQuery(k, minPathDistanceMeters);

        if (!query.isValid()) { // 基本校验
            return ResponseEntity.badRequest().build();
        }

        FrequentPathResult result = frequentPathService.analyzeFrequentPaths(query);
        return ResponseEntity.ok(result);
    }

    /**
     * F8: 获取区域 A 到区域 B 的 Top-K 频繁路径
     * 示例 URL: POST /api/paths/frequent/regional
     * Request Body (JSON):
     * {
     *   "k": 5,
     *   "regionA": { "minLongitude": ..., "minLatitude": ..., ... },
     *   "regionB": { ... }
     * }
     */
    @PostMapping("/frequent/regional")
    public ResponseEntity<FrequentPathResult> getRegionalFrequentPaths(
            @RequestBody FrequentPathQuery query // 接收包含 k, minDistance, regionA, regionB 的对象
    ) {
        System.out.println(query.getRegionB());
        query.setMinPathDistanceMeters(1000);
        // 校验 F8 特定条件
        if (!query.isRegionQuery() || !query.isValid()) {
            // 可以在这里返回更具体的错误信息
            return ResponseEntity.badRequest().build(); // Build a proper error response DTO later
        }

        FrequentPathResult result = frequentPathService.analyzeFrequentPaths(query);
        return ResponseEntity.ok(result);
    }
}