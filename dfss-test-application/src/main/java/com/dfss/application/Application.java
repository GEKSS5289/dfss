package com.dfss.application;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * Spring Boot 3.5.0 启动类
 */
@SpringBootApplication
@MapperScan("com.dfss.application.mapper")
@ComponentScan("com.dfss.data")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
