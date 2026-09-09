package com.shangyangcode.infinitechat.contactservice.constants;

public enum ConfigEnum {
    MEDIA_TYPE("application/json; charset=utf-8"),
    WORKED_ID("1"),
    DATACENTER_ID("1"),
    GROUP_AVATAR_URL("http://47.115.130.44/img/avatar/IM_GROUP.jpg"),
    REQUEST_SUCCESSFUL("请求成功"),
    OPTION_FAILURE("操作失败");


    private final String value;

    ConfigEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
