package com.shanyangcode.infinitechat.realtimecommunicationservice.constants;
public enum MessageRcvTypeEnum {
    TEXT_MESSAGE(1),
    PICTURE_MESSAGE(2),
    FILE_MESSAGE(3),
    VIDEO_MESSAGE(4),
    RED_PACKET_MESSAGE(5),
    EMOTICON_MESSAGE(6);


    private Integer code;
    MessageRcvTypeEnum(Integer code){
        this.code = code;
    }

    public static MessageRcvTypeEnum fromCode(int code) {
        for (MessageRcvTypeEnum type : MessageRcvTypeEnum.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }

    public Integer getCode(){
        return this.code;
    }
}