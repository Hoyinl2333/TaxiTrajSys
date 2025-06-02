package com.codex.taxitrajectory.model.query;

// 导入我们创建的自定义校验注解
import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.validation.ValidGeoBoundingBox;
import com.codex.taxitrajectory.model.validation.ValidTimeRange;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 包含用于区域关联分析（F5, F6功能）的各种查询参数模型。
 * 此类作为其内部静态查询类的容器。
 */
public class CorrelationQuery { // 类名已根据您的要求修改

    /**
     * F6功能（指定区域与其他区域的关联分析）的查询参数。
     * <p>
     * 地理边界约定：左上角（经度小，纬度大），右下角（经度大，纬度小）。
     * </p>
     */
    @Data
    @NoArgsConstructor
    @ValidTimeRange(message = "结束时间必须在开始时间之后或与开始时间相同")
    @ValidGeoBoundingBox(
            message = "区域坐标设置错误：左上角经度必须小于右下角经度，且右下角纬度必须小于左上角纬度",
            minLonFieldName = "topLeftLongitude",       // 左上角经度 (West)
            maxLonFieldName = "bottomRightLongitude",   // 右下角经度 (East)
            minLatFieldName = "bottomRightLatitude",    // 右下角纬度 (South)
            maxLatFieldName = "topLeftLatitude"       // 左上角纬度 (North)
    )
    public static class RegionSingleCorrelationQuery {

        /**
         * 分析的开始时间。
         */
        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;

        /**
         * 分析的结束时间。
         */
        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;

        /**
         * 时间槽的持续分钟数。
         */
        @NotNull(message = "时间槽分钟数不能为空")
        @Positive(message = "时间槽分钟数必须为正数")
        private Integer timeSlotMinutes;

        /**
         * 指定区域的左上角经度（通常是区域的西边界）。
         */
        @NotNull(message = "左上角经度不能为空")
        @DecimalMin(value = "-180.0", message = "左上角经度必须是有效的地理坐标值 [-180, 180]")
        @DecimalMax(value = "180.0", message = "左上角经度必须是有效的地理坐标值 [-180, 180]")
        private Double topLeftLongitude;

        /**
         * 指定区域的左上角纬度（通常是区域的北边界）。
         */
        @NotNull(message = "左上角纬度不能为空")
        @DecimalMin(value = "-90.0", message = "左上角纬度必须是有效的地理坐标值 [-90, 90]")
        @DecimalMax(value = "90.0", message = "左上角纬度必须是有效的地理坐标值 [-90, 90]")
        private Double topLeftLatitude;

        /**
         * 指定区域的右下角经度（通常是区域的东边界）。
         */
        @NotNull(message = "右下角经度不能为空")
        @DecimalMin(value = "-180.0", message = "右下角经度必须是有效的地理坐标值 [-180, 180]")
        @DecimalMax(value = "180.0", message = "右下角经度必须是有效的地理坐标值 [-180, 180]")
        private Double bottomRightLongitude;

        /**
         * 指定区域的右下角纬度（通常是区域的南边界）。
         */
        @NotNull(message = "右下角纬度不能为空")
        @DecimalMin(value = "-90.0", message = "右下角纬度必须是有效的地理坐标值 [-90, 90]")
        @DecimalMax(value = "90.0", message = "右下角纬度必须是有效的地理坐标值 [-90, 90]")
        private Double bottomRightLatitude;
    }
    /**
     * F5功能（两个指定区域间的关联分析）的查询参数。
     * 定义了进行此类分析所需的两个区域的定义、时间范围和时间槽间隔。
     * 通过内嵌的 {@link Region} 对象并配合 {@link jakarta.validation.Valid} 来校验区域边界。
     */
    @Data
    @NoArgsConstructor
    @ValidTimeRange(message = "结束时间必须在开始时间之后或与开始时间相同")
    public static class RegionCorrelationQuery {

        /**
         * 分析的开始时间。不能为空。
         */
        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;

        /**
         * 分析的结束时间。不能为空。
         */
        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;

        /**
         * 时间槽的持续分钟数。不能为空且必须为正数。
         */
        @NotNull(message = "时间槽分钟数不能为空")
        @Positive(message = "时间槽分钟数必须为正数")
        private Integer timeSlotMinutes;

        /**
         * 第一个分析区域。
         * 不能为空，且其内部定义的地理边界必须有效。
         */
        @NotNull(message = "区域1不能为空")
        @Valid // 级联校验 Region 对象内部的约束
        private Region region1;

        /**
         * 第二个分析区域。
         * 不能为空，且其内部定义的地理边界必须有效。
         */
        @NotNull(message = "区域2不能为空")
        @Valid // 级联校验 Region 对象内部的约束
        private Region region2;

        // 原来的topLeftLongitude1, topLeftLatitude1, ... bottomRightLatitude2 字段已移除
        // 构造函数也需要相应调整，或者依赖 Lombok 的 @AllArgsConstructor (如果添加的话)
        // 或者提供一个接收 Region 对象的构造函数

        /**
         * 全参数构造函数。
         * @param startTime 开始时间
         * @param endTime 结束时间
         * @param timeSlotMinutes 时间槽分钟数
         * @param region1 第一个区域对象
         * @param region2 第二个区域对象
         */
        public RegionCorrelationQuery(LocalDateTime startTime, LocalDateTime endTime, Integer timeSlotMinutes, Region region1, Region region2) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.timeSlotMinutes = timeSlotMinutes;
            this.region1 = region1;
            this.region2 = region2;
        }
    }
}
