package com.codex.taxitrajectory.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一的错误响应体封装类。
 * <p>
 * 用于在API发生错误时，向客户端返回结构化的错误信息。
 * 包含时间戳、HTTP状态码、错误类型、用户友好的消息、请求路径以及可选的详细错误列表。
 * </p>
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON输出时，值为null的字段将不会被包含
public class ErrorResponse {

    /**
     * 错误发生的时间戳。
     */
    private LocalDateTime timestamp;

    /**
     * HTTP状态码。
     */
    private int status;

    /**
     * 错误的简短描述 (例如 "Validation Error", "Bad Request")。
     */
    private String error;

    /**
     * 更详细、对用户友好的错误信息。
     */
    private String message;

    /**
     * 导致错误的请求路径。
     */
    private String path;

    /**
     * 可选的详细错误信息列表。
     * 例如，用于列出字段校验失败的具体信息。
     */
    private List<String> details;

    /**
     * 包含所有参数的构造函数。
     *
     * @param timestamp 错误发生的时间戳。
     * @param status    HTTP状态码。
     * @param error     错误的简短描述。
     * @param message   详细的错误信息。
     * @param path      导致错误的请求路径。
     * @param details   详细错误列表 (可为null)。
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, List<String> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    /**
     * 不包含详细错误列表的构造函数。
     *
     * @param timestamp 错误发生的时间戳。
     * @param status    HTTP状态码。
     * @param error     错误的简短描述。
     * @param message   详细的错误信息。
     * @param path      导致错误的请求路径。
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}