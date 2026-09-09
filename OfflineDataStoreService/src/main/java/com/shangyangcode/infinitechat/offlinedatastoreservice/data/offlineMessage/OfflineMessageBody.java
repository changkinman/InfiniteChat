package com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage;

import lombok.Data;

import java.io.Serializable;

@Data
public class OfflineMessageBody implements Serializable {
    private String content;

    private String createdAt;

    private String replyId;
}
