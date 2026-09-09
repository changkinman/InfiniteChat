package com.shanyangcode.infinitechat.authenticationservice.constants.config;

import lombok.Getter;

/** Configuration values that must be supplied by the deployment environment. */
@Getter
public enum ConfigEnum {
    SMS_ACCESS_KEY_ID("SMS access key id", environment("ALIYUN_SMS_ACCESS_KEY_ID")),
    SMS_ACCESS_KEY_SECRET("SMS access key secret", environment("ALIYUN_SMS_ACCESS_KEY_SECRET")),
    SMS_SIG_NAME("SMS sign name", environment("ALIYUN_SMS_SIGN_NAME")),
    SMS_TEMPLATE_CODE("SMS template code", environment("ALIYUN_SMS_TEMPLATE_CODE")),
    TOKEN_SECRET_KEY("token secret key", environment("JWT_SECRET_KEY"));

    private final String value;
    private final String text;

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
}
