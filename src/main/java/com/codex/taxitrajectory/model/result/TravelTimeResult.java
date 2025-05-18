package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// 每个时间段内的最短通行路径信息。代表的是在一个特定的时间段内，从区域 A 到区域 B 分析得出的最短路径和最短通行时间这一组结果。不直接传输给前端
@Data // 使用 @Data 注解
public class TravelTimeResult {

    private List<TaxiRecord> shortestPath; // 最短通行路径的轨迹点列表
    private Duration minTravelTime; // 最短通行时间
    private boolean found; // 是否找到从A到B的行程

    // 当找到行程时使用
    public TravelTimeResult(List<TaxiRecord> shortestPath, Duration minTravelTime) {
        this.shortestPath = shortestPath;
        this.minTravelTime = minTravelTime;
        this.found = true;
    }

    // 当没有找到行程时使用
    public TravelTimeResult() {
        this.found = false;
        this.shortestPath = null;
        this.minTravelTime = null;
    }

    // 静态工厂方法，用于创建“未找到行程”的实例，意图更清晰
    public static TravelTimeResult notFound() {
        TravelTimeResult result = new TravelTimeResult();
        result.setFound(false);
        return result;
    }

    /**
     * 获取格式化后的最短通行时间字符串。
     * Jackson会将其序列化为 "minTravelTimeFormatted": "X小时 Y分钟 Z秒" 等形式。
     *
     * @return String 格式化后的时间，如 "17分钟 8秒", "1小时", "30秒"。
     * 如果 minTravelTime 为 null, 则返回 null。
     * 如果时长为0，则返回 "0秒"。
     * 如果时长为负（异常情况），则返回 "无效时长"。
     */
    @JsonProperty("minTravelTimeFormatted")
    public String getMinTravelTimeFormatted() {
        if (this.minTravelTime == null) {
            return "N/A"; // 或者 "" 或 "N/A" 取决于您的偏好
        }

        long totalSeconds = this.minTravelTime.getSeconds();

        if (totalSeconds < 0) {
            return "无效时长"; // 通行时间通常不为负
        }
        if (totalSeconds == 0) {
            return "0秒"; // 明确处理0秒的情况
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();

        if (hours > 0) {
            parts.add(hours + "小时");
        }
        if (minutes > 0) {
            parts.add(minutes + "分钟");
        }
        if (seconds > 0) {
            parts.add(seconds + "秒");
        }

        // 如果 totalSeconds > 0，则 parts 列表至少会有一个元素
        // String.join 会用空格将列表中的元素连接起来
        return String.join(" ", parts);
    }
}