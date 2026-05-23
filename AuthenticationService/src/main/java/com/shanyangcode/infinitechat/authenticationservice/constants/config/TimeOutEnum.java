package com.shanyangcode.infinitechat.authenticationservice.constants.config;

import lombok.Getter;

@Getter
public enum TimeOutEnum {
    JWT_TIME_OUT("token time out(day)","jwt:", 24),
    CODE_TIME_OUT("code time out(minute)","sms:code:", 500000);

    private final String name;

    private final String prefix;

    private final int timeOut;

    TimeOutEnum(String name, String prefix, int timeOut) {
        this.name = name;
        this.prefix = prefix;
        this.timeOut = timeOut;
    }

}