package com.shangyangcode.infinitechat.offlinedatastoreservice.common;

import lombok.Data;

@Data
public class TextMessageBody {
    private String content;
    private Long replyId;
}
