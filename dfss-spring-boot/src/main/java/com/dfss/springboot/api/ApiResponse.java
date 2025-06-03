// 文件：dfss-spring-boot/src/main/java/com/dfss/springboot/api/ApiResponse.java
package com.dfss.springboot.api;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一 API 返回值载体。
 * @param <T> 返回数据的类型
 */
@Getter
@Setter
public class ApiResponse<T> {
    /**
     * HTTP 状态码，或自定义业务码
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 真正的数据
     */
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 快速构造成功且携带数据的响应 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /** 快速构造成功但无数据的响应 */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    /** 快速构造失败响应（使用业务码和错误消息） */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
