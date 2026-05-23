package com.shanyangcode.infinitechat.authenticationservice.constants.user;

import lombok.Getter;

@Getter
public enum ErrorEnum {
    // 400 01 02 03...
    // 500 01 02 03....

    SUCCESS(200, "ok"),
    REGISTER_ERROR(40001, "注册失败, 用户已存在"),
    CODE_ERROR(40002, "验证码错误"),
    LOGIN_ERROR(40003, "登录失败, 用户名或密码错误"),
    NO_USER_ERROR(40004, "用户不存在"),

    SYSTEM_ERROR(50000, "系统内部异常"),
    UPDATE_AVATAR_ERROR(50011, "更新头像失败");

    private final int code;
    private final String message;

    ErrorEnum(int code, String message){
        this.code = code;

        this.message = message;
    }
}
