// 文件：dfss-spring-boot/src/main/java/com/dfss/springboot/web/ApiLoggingInterceptor.java
package com.dfss.springboot.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



/**
 * 全局请求日志拦截器：在请求进入 Controller 前后打印日志。
 */
@Component
public class ApiLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        System.out.printf(">> [请求开始] %s %s%n", method, path);
        throw new RuntimeException("123");
//        return true; // 一定要返回 true，否则请求会被拦截住
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        int status = response.getStatus();
        System.out.printf("<< [请求结束] status=%d, URI=%s%n", status, request.getRequestURI());
    }
}
