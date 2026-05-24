package com.shanyangcode.infinitechat.authenticationservice.constants.config;

import lombok.Getter;

@Getter
public enum ConfigEnum {

    SMS_SIG_NAME("smsSigName","无夕教育科技"),
    TOKEN_SECRET_KEY("tokenSecretKey","goat");

    private final String value;
    private final String text;

    ConfigEnum(String text, String value){
        this.text = text;
        this.value = value;
    }
}