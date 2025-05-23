package com.codex.taxitrajectory.exception; // 确认包名

import com.codex.taxitrajectory.model.error.ErrorResponse; // 导入ErrorResponse DTO，路径应为 com.codex.taxitrajectory.model.error
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 使用 @ControllerAdvice 注解，使其能够处理在Controller层抛出的各种异常，
 * 并为客户端返回统一格式的错误响应。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理Spring MVC在参数绑定或类型转换失败时抛出的异常。
     * 例如，将路径变量或请求参数转换为错误的类型（如期望数字但得到字符串）。
     * @param ex      捕获到的 TypeMismatchException 实例。
     * @param request 当前的Web请求。
     * @return 包含错误信息的 ResponseEntity，HTTP状态码为400 (Bad Request)。
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            TypeMismatchException ex, WebRequest request) {
        String fieldName = "未知参数"; // 默认值
        Object rejectedValue = ex.getValue();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知类型";


        if (ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
            fieldName = ((org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) ex).getName();
        } else if (ex.getPropertyName() != null) { //
            fieldName = ex.getPropertyName();
        }


        String detailMessage = String.format("参数 '%s' 的值 '%s' 格式不正确，期望的类型是 '%s'。",
                fieldName,
                rejectedValue,
                requiredType
        );

        List<String> details = Collections.singletonList(detailMessage);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Type Mismatch", // 错误类型
                "请求中的参数类型不匹配或格式错误。", // 通用消息
                request.getDescription(false).replace("uri=", ""), // 请求路径
                details
        );
        // 日志中仍然可以记录原始异常的完整消息
        logger.warn("参数类型不匹配: Path=[{}], Parameter=[{}], Value=[{}], RequiredType=[{}], ErrorMessage=[{}]",
                errorResponse.getPath(), fieldName, rejectedValue, requiredType, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理JSR 303 Bean Validation（@Valid @RequestBody）失败时抛出的 MethodArgumentNotValidException。
     * @param ex      捕获到的 MethodArgumentNotValidException 实例。
     * @param request 当前的Web请求。
     * @return 包含详细校验错误信息的 ResponseEntity，HTTP状态码为400 (Bad Request)。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        // 如果没有字段错误（例如，由类级别的自定义注解抛出），则获取全局错误
        if (details.isEmpty()) {
            details = ex.getBindingResult()
                    .getGlobalErrors()
                    .stream()
                    .map(error -> error.getObjectName() + ": " + error.getDefaultMessage()) // 对于类级别注解，objectName是DTO名
                    .collect(Collectors.toList());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed", // 错误类型
                "请求参数校验失败，请检查提交的数据。", // 通用消息
                request.getDescription(false).replace("uri=", ""), // 请求路径
                details // 具体的字段或对象校验失败信息
        );
        logger.warn("参数校验失败: Path=[{}], Details=[{}]", errorResponse.getPath(), details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理JSR 303注解校验 @RequestParam, @PathVariable 等参数失败时抛出的 ConstraintViolationException。
     * @param ex      捕获到的 ConstraintViolationException 实例。
     * @param request 当前的Web请求。
     * @return 包含详细约束违反信息的 ResponseEntity，HTTP状态码为400 (Bad Request)。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation", // 错误类型
                "请求参数约束校验失败。", // 通用消息
                request.getDescription(false).replace("uri=", ""), // 请求路径
                details // 具体的约束违反信息
        );
        logger.warn("参数约束违反: Path=[{}], Details=[{}]", errorResponse.getPath(), details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    /**
     * 处理业务逻辑或参数问题明确抛出的 IllegalArgumentException。
     * @param ex      捕获到的 IllegalArgumentException 实例。
     * @param request 当前的Web请求。
     * @return 包含错误信息的 ResponseEntity，HTTP状态码为400 (Bad Request)。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request", // 错误类型
                ex.getMessage(), // 直接使用异常中定义的消息，期望Service层提供对用户友好的消息
                request.getDescription(false).replace("uri=", "") // 请求路径
        );
        logger.warn("非法参数请求: Path=[{}], Message=[{}]", errorResponse.getPath(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有其他未被特定处理器捕获的服务器内部异常。
     * @param ex      捕获到的 Exception 实例。
     * @param request 当前的Web请求。
     * @return 通用的服务器内部错误 ResponseEntity，HTTP状态码为500 (Internal Server Error)。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {

        // 对于未知异常，记录完整的堆栈信息，这对于调试非常重要
        logger.error("发生未处理的服务器内部错误: Path=[{}]", request.getDescription(false).replace("uri=", ""), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error", // 错误类型
                "服务器内部发生未知错误，请稍后重试或联系管理员。", // 对用户隐藏具体错误细节
                request.getDescription(false).replace("uri=", "") // 请求路径
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}