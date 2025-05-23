package com.codex.taxitrajectory.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON输出时，值为null的字段将不会被包含
public class ErrorResponse {

    private LocalDateTime timestamp; // 错误发生的时间戳
    private int status; // HTTP状态码
    private String error; // 错误的简短描述 (例如 "Validation Error", "Bad Request")
    private String message; // 更详细、对用户友好的错误信息
    private String path; // 导致错误的请求路径
    private List<String> details; // 可选，用于列出字段校验失败的具体信息

    // 包含所有参数的构造函数
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, List<String> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    // 不包含details的构造函数 (用于更通用的错误)
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}