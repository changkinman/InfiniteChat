package com.shanyangcode.infinitechat.realtimecommunicationservice.constants;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ConfigEnum {


    TOKEN_SECRET_KEY("tokenSecretKey","goat"),
    PASSWORD_SALT("passwordSalt","goat"),
    WX_STATE("wxState","goat"),
    WORKED_ID("workedId","1"),
    DATACENTER_ID("DATACENTER_ID","1"),
    IMAGE_URI("imageUri","http://127.0.0.1/img/avatar/"),
    IMAGE_PATH("imagePath", "/home/img/avatar/"),
    NETTY_SERVER_HEAD("nettyServerHead","Nacos:"),
    REDIS_CONVERT_SEND("redisConvertSend","userLogout");

    private final String text;

    private final String value;

    ConfigEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }


    public static List<String> getValues() {
          return Arrays.stream(ConfigEnum.values()).map(ConfigEnum::getValue).collect(Collectors.toList());
    }


    public static ConfigEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ConfigEnum anEnum : ConfigEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }

        }
        return null;
    }
    public String getText() {
        return text;
    }


    public String getValue() {
        return value;
    }


}
