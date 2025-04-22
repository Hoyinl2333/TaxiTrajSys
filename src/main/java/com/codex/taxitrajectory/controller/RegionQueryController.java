package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.result.RegionQueryResult;
import com.codex.taxitrajectory.service.RegionQueryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/region")
public class RegionQueryController {

    private final RegionQueryService regionQueryService;

    public RegionQueryController(RegionQueryService regionQueryService) {
        this.regionQueryService = regionQueryService;
    }

    /**
     * F3: 区域范围查找 - 使用 Query 类作为请求体
     */
    @PostMapping("/count")
    public RegionQueryResult getTaxisInRegion(@RequestBody @Valid RegionQuery query) {
        query.validate();
        return regionQueryService.getTaxisInRegion(query);
    }

    /**
     * F3: 区域范围查找 - 同时支持 URL 参数方式（向后兼容）
     */
//    @GetMapping("/count")
//    public ResponseEntity<Integer> countTaxisInRegion(
//            @RequestParam double lon1,
//            @RequestParam double lat1,
//            @RequestParam double lon2,
//            @RequestParam double lat2,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
//
//        // 创建 RegionQuery 对象，自动计算经纬度区域边界
//        RegionQuery query = new RegionQuery(
//                Math.min(lon1, lon2),
//                Math.min(lat1, lat2),
//                Math.max(lon1, lon2),
//                Math.max(lat1, lat2),
//                startTime,
//                endTime
//        );
//        query.validate();
//        int taxiCount = regionQueryService.countTaxisInRegion(query);
//        return ResponseEntity.ok(taxiCount);
//    }
}
