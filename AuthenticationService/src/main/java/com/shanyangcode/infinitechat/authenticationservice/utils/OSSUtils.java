package com.shanyangcode.infinitechat.authenticationservice.utils;

import cn.hutool.core.util.StrUtil;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class OSSUtils {
    @Resource
    private MinioClient minioClient;

    @Value("${minio.url}")
    private String url;

    @SneakyThrows
    public String uploadUrl(String bucketName, String objectName, Integer expires) {

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expires, TimeUnit.SECONDS)
                        .build());
    }

    public String downUrl(String bucketName, String fileName) {

        return url + StrUtil.SLASH + bucketName + StrUtil.SLASH + fileName;
    }
}
