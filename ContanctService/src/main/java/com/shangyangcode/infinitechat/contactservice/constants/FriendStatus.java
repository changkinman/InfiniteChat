package com.shangyangcode.infinitechat.contactservice.constants;

/**
 * 好友状态枚举
 */
public enum FriendStatus {
    NORMAL(1, "好友"),
    BLACKLISTED(2, "拉黑"),
    DELETED(3, "删除");

    private final int value;
    private final String description;

    FriendStatus(int value, String description) {
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