// 文件：dfss-spring-boot/src/main/java/com/dfss/springboot/web/WebMvcConfig.java
package com.dfss.springboot.web;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web MVC 配置：注册自定义拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private ApiLoggingInterceptor apiLoggingInterceptor;



    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(apiLoggingInterceptor)
                .addPathPatterns("/**")     // 拦截所有路径
                .excludePathPatterns("/error"); // 排除 Spring Boot 默认的 /error 端点
    }
}
