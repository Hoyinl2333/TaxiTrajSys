package com.codex.taxitrajectory.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

/**
 * {@link ValidGeoBoundingBox} 注解的校验器实现。
 * 负责比较对象中地理边界字段，确保经纬度顺序正确。
 */
public class ValidGeoBoundingBoxValidator implements ConstraintValidator<ValidGeoBoundingBox, Object> {

    private String minLonFieldName;
    private String maxLonFieldName;
    private String minLatFieldName;
    private String maxLatFieldName;

    @Override
    public void initialize(ValidGeoBoundingBox constraintAnnotation) {
        this.minLonFieldName = constraintAnnotation.minLonFieldName();
        this.maxLonFieldName = constraintAnnotation.maxLonFieldName();
        this.minLatFieldName = constraintAnnotation.minLatFieldName();
        this.maxLatFieldName = constraintAnnotation.maxLatFieldName();
    }

    /**
     * 执行校验逻辑。
     * @param value 被校验的对象。
     * @param context 约束校验上下文。
     * @return 如果地理边界框有效或相关字段为null，则返回true；否则返回false。
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 对象为null，不执行此校验
        }

        final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

        try {
            Object minLonObj = beanWrapper.getPropertyValue(minLonFieldName);
            Object maxLonObj = beanWrapper.getPropertyValue(maxLonFieldName);
            Object minLatObj = beanWrapper.getPropertyValue(minLatFieldName);
            Object maxLatObj = beanWrapper.getPropertyValue(maxLatFieldName);

            // 字段本身的null由@NotNull处理
            if (minLonObj == null || maxLonObj == null || minLatObj == null || maxLatObj == null) {
                return true;
            }

            if (!(minLonObj instanceof Double) || !(maxLonObj instanceof Double) ||
                    !(minLatObj instanceof Double) || !(maxLatObj instanceof Double)) {
                return false; // 类型不匹配
            }

            double minLon = (Double) minLonObj;
            double maxLon = (Double) maxLonObj;
            double minLat = (Double) minLatObj;
            double maxLat = (Double) maxLatObj;

            // 核心校验：确保最小值小于最大值
            return minLon < maxLon && minLat < maxLat;

        } catch (Exception e) {
            // 属性访问异常等，视为校验失败
            return false;
        }
    }
}