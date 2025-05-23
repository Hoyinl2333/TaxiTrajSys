package com.codex.taxitrajectory.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义约束注解，用于校验地理边界框的有效性。
 * 确保最小经度小于最大经度，最小纬度小于最大纬度。
 * 应用于类级别。
 */
@Documented
@Constraint(validatedBy = ValidGeoBoundingBoxValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGeoBoundingBox {

    /**
     * 校验失败时的错误消息。
     */
    String message() default "无效的地理边界框：最小经纬度必须小于对应的最大经纬度";

    /**
     * JSR 303标准属性：校验组。
     */
    Class<?>[] groups() default {};

    /**
     * JSR 303标准属性：Payload。
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 最小经度字段的名称。
     */
    String minLonFieldName() default "minLongitude";
    /**
     * 最大经度字段的名称。
     */
    String maxLonFieldName() default "maxLongitude";
    /**
     * 最小纬度字段的名称。
     */
    String minLatFieldName() default "minLatitude";
    /**
     * 最大纬度字段的名称。
     */
    String maxLatFieldName() default "maxLatitude";
}