package com.shangyangcode.infinitechat.contactservice.constants;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    NORMAL(1, "正常"),
    DELETED(2, "删除");

    private final int value;
    private final String description;

    UserStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
