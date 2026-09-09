package com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OfflineMessage implements Serializable {
    private Long total;

    private String sessionId;

    private String sessionName;

    private String sessionAvatar;

    private Integer sessionType;

    private List<OfflineMessageDetail> offlineMessageDetails;
}
