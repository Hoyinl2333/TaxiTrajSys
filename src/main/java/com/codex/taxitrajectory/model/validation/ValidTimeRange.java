package com.codex.taxitrajectory.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义约束注解，用于校验指定的开始时间字段不晚于结束时间字段。
 * 应用于类级别。
 */
@Documented
@Constraint(validatedBy = ValidTimeRangeValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimeRange {

    /**
     * 校验失败时的错误消息。
     */
    String message() default "结束时间必须在开始时间之后或与开始时间相同";

    /**
     * JSR 303标准属性：校验组。
     */
    Class<?>[] groups() default {};

    /**
     * JSR 303标准属性：Payload。
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 开始时间字段的名称。
     */
    String startTimeFieldName() default "startTime";

    /**
     * 结束时间字段的名称。
     */
    String endTimeFieldName() default "endTime";
}