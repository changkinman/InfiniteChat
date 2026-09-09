package com.shangyangcode.infinitechat.offlinedatastoreservice.constants.config;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ConfigEnum {

    WORKED_ID("WORKED_Id","1"),
    DATACENTER_ID("DATACENTER_ID","1"),
    GROUP_TYPE("GROUP_TYPE","2"),
    MESSAGE_TYPE("MESSAGE_TYPE","5"),
    GROUP_AVATAR("GROUP_AVATAR","http://47.115.130.44/img/avatar/IM_GROUP.jpg");



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
