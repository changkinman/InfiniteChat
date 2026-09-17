package com.shanyangcode.infinitechat.messageingservice;

import java.time.Clock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(basePackages = "com.shanyangcode.infinitechat.messageingservice.feign")
@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
@MapperScan("com.shanyangcode.infinitechat.messageingservice.mapper")
public class MessageingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageingServiceApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
