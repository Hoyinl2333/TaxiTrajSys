package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.validation.ValidTimeRange; // 导入自定义时间范围注解
import jakarta.validation.Valid; // 导入 @Valid 用于级联校验
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor; // 添加无参构造，便于框架使用

import java.time.LocalDateTime;

/**
 * F9 通行时间分析的查询参数。
 * 定义了分析从区域A到区域B最短通行时间所需的区域信息和时间范围。
 * 使用JSR 303注解及自定义注解进行参数校验。
 */
@Data
@NoArgsConstructor // 添加无参构造函数
@ValidTimeRange
public class TravelTimeQuery {

    /**
     * 起始区域 A。
     * 不能为空，且其内部定义的地理边界必须有效。
     */
    @NotNull(message = "起始区域A不能为空")
    @Valid
    private Region regionA;

    /**
     * 结束区域 B。
     * 不能为空，且其内部定义的地理边界必须有效。
     */
    @NotNull(message = "结束区域B不能为空")
    private Region regionB;

    /**
     * 查询的开始时间。
     * 不能为空。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 查询的结束时间。
     * 不能为空，并且必须在开始时间之后或与开始时间相同。
     */
    @NotNull(message = "结束时间不能为空")
    @Valid
    private LocalDateTime endTime;

    /**
     * 全参数构造函数。
     * @param regionA   起始区域A
     * @param regionB   结束区域B
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public TravelTimeQuery(Region regionA, Region regionB, LocalDateTime startTime, LocalDateTime endTime) {
        this.regionA = regionA;
        this.regionB = regionB;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}