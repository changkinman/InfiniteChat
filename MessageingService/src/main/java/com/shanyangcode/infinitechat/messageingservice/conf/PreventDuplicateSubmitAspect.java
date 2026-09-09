package com.shanyangcode.infinitechat.messageingservice.conf;

import cn.hutool.log.Log;
import com.shanyangcode.infinitechat.messageingservice.util.PreventDuplicateSubmit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.rmi.ServerException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@Aspect
public class PreventDuplicateSubmitAspect {
    private final Map<String, Long> requestCache = new ConcurrentHashMap<>();

    public Object preventDuplicate(ProceedingJoinPoint joinPoint, PreventDuplicateSubmit preventDuplicateSubmit) throws ServerException {
        // 根据方法名和参数生成唯一键
        String key = joinPoint.getSignature().toShortString() + Arrays.toString(joinPoint.getArgs());

        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = requestCache.get(key);

        if(lastRequestTime != null && (currentTime - lastRequestTime) < preventDuplicateSubmit.timeout()){
            log.warn("Duplicate submission detected for method: {}", key);
            throw new ServerException("请勿重复提交请求");
        }

        requestCache.put(key, currentTime);

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}