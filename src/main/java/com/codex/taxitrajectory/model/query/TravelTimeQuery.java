package com.codex.taxitrajectory.model.query;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox; // 确保导入路径正确
import com.codex.taxitrajectory.model.validation.ValidTimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * F9 通行时间分析的查询参数。
 * <p>
 * 定义了分析从区域A到区域B最短通行时间所需的区域信息和时间范围。
 * 使用JSR 303注解及自定义注解 ({@link ValidTimeRange}, {@link ValidGeoBoundingBox}) 进行参数校验。
 * </p>
 */
@Data
@NoArgsConstructor    // 为JSON反序列化等提供无参构造函数
@AllArgsConstructor   // 提供所有字段的构造函数
@ValidTimeRange       // 自定义校验：确保startTime不晚于endTime

// 移除这里的类级别 @ValidGeoBoundingBox，依赖于 Region 类自身的校验。TODO:或许还需要检查两个区域有没有重叠，这个取决于需求
public class TravelTimeQuery {

    /**
     * 起始区域 A。
     * 不能为空，且其内部定义的地理边界必须有效 (通过 {@link Valid} 触发 {@link Region} 内部的校验)。
     */
    @NotNull(message = "起始区域A不能为空")
    @Valid // 级联校验RegionA对象内部的约束
    private Region regionA;

    /**
     * 结束区域 B。
     * 不能为空，且其内部定义的地理边界必须有效 (通过 {@link Valid} 触发 {@link Region} 内部的校验)。
     */
    @NotNull(message = "结束区域B不能为空")
    @Valid // 级联校验RegionB对象内部的约束
    private Region regionB;

    /**
     * 查询的开始时间。
     * 不能为空。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 查询的结束时间。
     * 不能为空，并且必须在开始时间之后或与开始时间相同 (通过类级别的 {@link ValidTimeRange} 校验)。
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;


}