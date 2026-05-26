package com.shanyangcode.infinitechat.messageingservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.shanyangcode.infinitechat.messageingservice.feign")
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.shanyangcode.infinitechat.messageingservice.mapper")
public class MessageingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageingServiceApplication.class, args);
    }

}
