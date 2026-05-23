package com.shanyangcode.infinitechat.authenticationservice.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;

@Configuration
public class Interceptors implements WebMvcConfigurer {

    @Autowired
    private SourceHandler sourceHandler;

    @Autowired
    private JwtHandler jwtHandler;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册⾃定义拦截器对象
        registry.addInterceptor(sourceHandler)
                .addPathPatterns("/**");//设置拦截器拦截的请求路径（ /** 表⽰拦截有请求）

        registry.addInterceptor(jwtHandler)
                .addPathPatterns(new ArrayList<String>(Arrays.asList("/api/v1/user/avatar")));
    }
}