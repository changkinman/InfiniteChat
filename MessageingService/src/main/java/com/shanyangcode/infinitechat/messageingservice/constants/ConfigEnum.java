package com.shanyangcode.infinitechat.messageingservice.constants;

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
    MEDIA_TYPE("mediaType","application/json; charset=utf-8"),
    MSG_URL("msgUrl","/api/v1/message/user/"), //RealTimeCommunicationService服务推送接口
    KAFKA_TOPICS("kafkaTopics","thousands_word_message"),
    HTTP_CONFIG("httpConfig","application/json; charset=utf-8"),
    IMAGE_PATH("imagePath", "/home/img/avatar/");

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
