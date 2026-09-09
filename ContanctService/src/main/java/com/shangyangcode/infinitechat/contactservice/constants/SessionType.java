package com.shangyangcode.infinitechat.contactservice.constants;

public enum SessionType {
    // 单聊
    SINGLE(1),

    //群聊
    GROUP(2);
    private int value;

    SessionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
