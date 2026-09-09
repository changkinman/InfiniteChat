package com.shanyangcode.infinitechat.momentservice.constants;

public enum ConfigEnum {
    SMS_ACCESS_KEY_ID(environment("MOMENT_ALIYUN_SMS_ACCESS_KEY_ID")),
    SMS_ACCESS_KEY_SECRET(environment("MOMENT_ALIYUN_SMS_ACCESS_KEY_SECRET")),
    SMS_SIG_NAME("无夕教育科技"),
    SMS_TEMPLATE_CODE("SMS_471490089"),
    TOKEN_SECRET_KEY(environment("MOMENT_TOKEN_SECRET_KEY")),
    PASSWORD_SALT(environment("MOMENT_PASSWORD_SALT")),
    WX_STATE("goat"),
    WORKED_ID("1"),
    DATACENTER_ID("1"),
    IMAGE_URI("http://118.25.77.201:9000/infinitec-chat/"),
    IMAGE_PATH("/home/img/avatar"),
    NOTICE_URL("/api/v1/message/push/moment"),
    MEDIA_TYPE("application/json; charset=utf-8"),
    MINIO_SERVER_URL("http://118.25.77.201:9000"),
    MINIO_ACCESS_KEY(environment("MOMENT_MINIO_ACCESS_KEY")),
    MINIO_SECRET_KEY(environment("MOMENT_MINIO_SECRET_KEY")),
    REQUEST_SUCCESSFUL("请求成功"),
    MINIO_BUCKET_NAME("infinitec-chat");

    private final String value;

    ConfigEnum(String value) {
        this.value = value;
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    public String getValue() {
        return value;
    }
}
