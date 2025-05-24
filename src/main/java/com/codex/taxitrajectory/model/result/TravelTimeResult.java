package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
// 无需手动导入 AllArgsConstructor 和 NoArgsConstructor，@Data 会处理部分或全部（取决于字段类型）
// 但为了明确意图和满足某些框架需求，通常会显式添加 @NoArgsConstructor
// @AllArgsConstructor 可以按需添加，这里根据您的统一要求添加
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * F9 通行时间分析的结果。
 * <p>
 * 封装了在特定时间段内，从区域A到区域B分析得出的最短通行路径、时间及是否找到路径的标记。
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数
@AllArgsConstructor   // 提供全参构造函数
public class TravelTimeResult {

    /**
     * 最短通行路径的轨迹点列表。
     * 如果未找到路径 (found=false)，则此字段为 null。
     */
    private List<TaxiRecord> shortestPath;

    /**
     * 最短通行时间。
     * 如果未找到路径 (found=false)，则此字段为 null。
     */
    private Duration minTravelTime;

    /**
     * 标记是否找到了从区域A到区域B的有效行程。
     */
    private boolean found;


    /**
     * 获取格式化后的最短通行时间字符串，用于JSON序列化。
     * <p>
     * 例如："17分钟 8秒", "1小时", "30秒"。
     * </p>
     *
     * @return 格式化后的时间字符串。如果 {@code minTravelTime} 为 null，返回 "N/A"；
     * 如果时长为0，返回 "0秒"；如果时长为负（异常情况），返回 "无效时长"。
     */
    @JsonProperty("minTravelTimeFormatted")
    public String getMinTravelTimeFormatted() {
        if (this.minTravelTime == null) {
            return "N/A";
        }

        long totalSeconds = this.minTravelTime.getSeconds();

        if (totalSeconds < 0) {
            return "无效时长";
        }
        if (totalSeconds == 0) {
            return "0秒";
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
        // 即使小时和分钟都为0，如果总秒数大于0，也应显示秒
        if (seconds > 0 || parts.isEmpty()) { // 如果parts为空（即小时和分钟都为0），则必须显示秒
            parts.add(seconds + "秒");
        }

        return String.join(" ", parts);
    }
}