package com.shangyangcode.infinitechat.contactservice.constants;

public enum FriendApplicationStatus {
    UNREAD(0, "未读"),
    ACCEPTED(1, "通过"),
    REJECTED(2, "拒绝"),
    READ(3, "已读"),
    EXPIRED(4, "过期");

    private final int code;
    private final String description;

    FriendApplicationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FriendApplicationStatus fromCode(int code) {
        for (FriendApplicationStatus status : FriendApplicationStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的状态码: " + code);
    }
}
