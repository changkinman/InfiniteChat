package com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage;

import lombok.Data;

import java.io.Serializable;

@Data
public class OfflineMessageDetail implements Serializable {

    private String avatar;

    private OfflineMessageBody offlineMessageBody;

    private Integer type;

    private String userName;

    private String sendUserId;

    private String messageId;
}
