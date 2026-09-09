package com.shanyangcode.infinitechat.realtimecommunicationservice.constants;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ConfigEnum {

    SMS_ACCESS_KEY_ID("smsAccessKeyId", environment("RTC_ALIYUN_SMS_ACCESS_KEY_ID")),
    SMS_ACCESS_KEY_SECRET("smsAccessKeySecret", environment("RTC_ALIYUN_SMS_ACCESS_KEY_SECRET")),
    SMS_SIG_NAME("smsSigName", "Zzw"),
    SMS_TEMPLATE_CODE("smsTemplateCode", "SMS_468395208"),
    TOKEN_SECRET_KEY("tokenSecretKey", environment("RTC_TOKEN_SECRET_KEY")),
    PASSWORD_SALT("passwordSalt", environment("RTC_PASSWORD_SALT")),
    WX_STATE("wxState", "goat"),
    WORKED_ID("workedId", "1"),
    DATACENTER_ID("DATACENTER_ID", "1"),
    IMAGE_URI("imageUri", "http://47.113.96.105/img/avatar/"),
    IMAGE_PATH("imagePath", "/home/img/avatar/"),
    NETTY_SERVER_HEAD("nettyServerHead", "Nacos:"),
    REDIS_CONVERT_SEND("redisConvertSend", "userLogout");

    private final String text;
    private final String value;

    ConfigEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    public static List<String> getValues() {
        return Arrays.stream(ConfigEnum.values()).map(ConfigEnum::getValue).collect(Collectors.toList());
    }

    public static ConfigEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ConfigEnum anEnum : ConfigEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }
}
