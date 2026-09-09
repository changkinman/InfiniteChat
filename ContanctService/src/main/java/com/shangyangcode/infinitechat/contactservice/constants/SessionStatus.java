package com.shangyangcode.infinitechat.contactservice.constants;


import lombok.Getter;

/**
 * 群组状态枚举
 */
@Getter
public enum SessionStatus {
    NORMAL(1, "正常"),
    DELETED(2, "删除");

    private final int value;
    private final String description;

    SessionStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

}
