package com.dfss.springboot.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 全局请求日志拦截器：在请求进入 Controller 前后打印日志。
 */
@Component
@Slf4j
public class ApiLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info(">> [请求开始] {}-{}", method, path);
        return true;
    }



    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        int status = response.getStatus();
        log.info("<< [请求结束] status={}, URI={}", status, request.getRequestURI());
    }
}
