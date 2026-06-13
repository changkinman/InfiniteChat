package com.shanyangcode.infinitechat.momentservice.constants;

import lombok.Getter;

@Getter
public enum ErrorEnum {
    // 400 01 02 03...
    // 500 01 02 03....

    SUCCESS(200, "ok"),
    NO_USER_ERROR(40004, "用户不存在"),
    DELETE_MOMENT_FAILED_MSG(40005, "删除失败, 朋友圈不存在"),
    DELETE_Like_FAILED_MSG(40005, "删除失败, 点赞不存在"),
    DELETE_MOMENT_COMMENT_FAIL_msg(40006, "删除失败，朋友圈评论不存在"),

    SYSTEM_ERROR(50000, "系统内部异常"),
    SERVICE_UNAVAILABLE(50001, "实时通信服务不可用"),
    MESSAGE_SEND_FAILURE(50002, "消息发送失败"),
    UPDATE_AVATAR_ERROR(50011, "更新头像失败"),
    ERROR_UPLOAD_FAILED(50012,"上传图片失败" ),

    DATABASE_ERROR(50013, "数据库异常");



    private final int code;
    private final String message;

    ErrorEnum(int code, String message) {
        this.code = code;

        this.message = message;
    }
}
