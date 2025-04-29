package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import lombok.Data;

import java.time.Duration;
import java.util.List;

// 每个时间段内的最短通行路径信息。代表的是在一个特定的时间段内，从区域 A 到区域 B 分析得出的最短路径和最短通行时间这一组结果。不直接传输给前端
@Data // 使用 @Data 注解
public class ShortestPathInfo {

    private List<TaxiRecord> shortestPath; // 最短通行路径的轨迹点列表
    private Duration minTravelTime; // 最短通行时间
    private boolean found; // 是否找到从A到B的行程

    // 当找到行程时使用
    public ShortestPathInfo(List<TaxiRecord> shortestPath, Duration minTravelTime) {
        this.shortestPath = shortestPath;
        this.minTravelTime = minTravelTime;
        this.found = true;
    }

    // 当没有找到行程时使用
    public ShortestPathInfo() {
        this.found = false;
        this.shortestPath = null;
        this.minTravelTime = null;
    }
}