package com.codex.taxitrajectory.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import org.springframework.beans.BeanWrapperImpl;

/**
 * {@link ValidTimeRange} 注解的校验器实现。
 * 负责比较对象中两个LocalDateTime类型的字段，确保时间顺序正确。
 */
public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {

    private String startTimeFieldName;
    private String endTimeFieldName;

    @Override
    public void initialize(ValidTimeRange constraintAnnotation) {
        this.startTimeFieldName = constraintAnnotation.startTimeFieldName();
        this.endTimeFieldName = constraintAnnotation.endTimeFieldName();
    }

    /**
     * 执行校验逻辑。
     * @param value 被校验的对象。
     * @param context 约束校验上下文。
     * @return 如果时间范围有效或相关字段为null，则返回true；否则返回false。
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 对象为null，不执行此校验
        }

        try {
            final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);
            Object startTimeObj = beanWrapper.getPropertyValue(startTimeFieldName);
            Object endTimeObj = beanWrapper.getPropertyValue(endTimeFieldName);

            // 字段本身的null由@NotNull处理，这里假设如果它们为null，则此特定校验通过
            if (startTimeObj == null || endTimeObj == null) {
                return true;
            }

            if (!(startTimeObj instanceof LocalDateTime) || !(endTimeObj instanceof LocalDateTime)) {
                return false; // 类型不匹配
            }

            LocalDateTime startTime = (LocalDateTime) startTimeObj;
            LocalDateTime endTime = (LocalDateTime) endTimeObj;

            return !startTime.isAfter(endTime); // 开始时间不晚于结束时间

        } catch (Exception e) {
            // 属性访问异常等，视为校验失败
            return false;
        }
    }
}