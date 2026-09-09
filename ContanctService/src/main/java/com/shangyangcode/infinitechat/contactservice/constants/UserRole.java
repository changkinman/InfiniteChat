package com.shangyangcode.infinitechat.contactservice.constants;

/**
 * 用户角色枚举
 */
public enum UserRole {
    GROUP_OWNER(1, "群主"),
    GROUP_ADMIN(2, "管理员"),
    GROUP_MEMBER(3, "普通成员");

    private final int value;
    private final String description;

    UserRole(int value, String description) {
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