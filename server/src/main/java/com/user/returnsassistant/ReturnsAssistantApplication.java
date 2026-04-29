package com.user.returnsassistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.user.returnsassistant.mapper")
@SpringBootApplication
public class ReturnsAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReturnsAssistantApplication.class, args);
    }
}
