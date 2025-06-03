// 文件：dfss-spring-boot/src/main/java/com/dfss/springboot/web/GlobalExceptionHandler.java
package com.dfss.springboot.web;

import com.dfss.common.exceptions.DataBaseOperationException;
import com.dfss.springboot.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;



/**
 * 全局异常处理器：将抛出的异常统一封装为 ApiResponse。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 处理自定义的 DataBaseOperationException */
    @ExceptionHandler(DataBaseOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDbOpException(DataBaseOperationException ex) {
        // ex.getErrorCode() 对应 DataBaseErrorCode 的 code
        String msg = "[" + ex.getErrorCode() + "] " + ex.getMessage();
        return ApiResponse.fail(400, msg);
    }

    /** 处理所有未捕获的 RuntimeException */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        // 可以打印日志：日志框架自行决定
        ex.printStackTrace();
        return ApiResponse.fail(500, "服务内部错误: " + ex.getMessage());
    }

    /** 处理参数校验异常（@Valid 注解产生的） */
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(Exception ex) {
        String msg;
        if (ex instanceof MethodArgumentNotValidException manv) {
            msg = manv.getBindingResult().getFieldErrors()
                    .stream()
                    .map(err -> err.getField() + " " + err.getDefaultMessage())
                    .findFirst()
                    .orElse("参数格式错误");
        } else if (ex instanceof BindException be) {
            msg = be.getBindingResult().getFieldErrors()
                    .stream()
                    .map(err -> err.getField() + " " + err.getDefaultMessage())
                    .findFirst()
                    .orElse("参数绑定错误");
        } else {
            msg = "参数校验失败";
        }
        return ApiResponse.fail(400, msg);
    }

    /** 捕获 404（NoHandlerFoundException） */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(HttpServletRequest req, NoHandlerFoundException ex) {
        return ApiResponse.fail(404, "资源未找到: " + req.getRequestURI());
    }
}
