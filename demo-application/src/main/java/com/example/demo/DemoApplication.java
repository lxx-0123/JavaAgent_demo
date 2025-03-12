package com.example.demo;

import com.example.agent.transformer.ApiInfoCache;
import com.example.demo.controller.ApiService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
//        SpringApplication.run(DemoApplication.class, args);
        ApplicationContext context = SpringApplication.run(DemoApplication.class, args);
        // 获取 ApiService 实例并执行扫描
        ApiService apiService = context.getBean(ApiService.class);
        apiService.getApiOnStartup();
    }

//    @Bean
//    public CommandLineRunner commandLineRunner(ApiService apiService) {
//        return args -> {
//            // 启动时获取API信息
//            System.out.println("应用启动时获取API信息：");
//            apiService.getApiOnStartup();
//        };
//    }
} 