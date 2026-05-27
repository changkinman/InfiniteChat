package com.shanyangcode.infinitechat.messageingservice.data.senRedPackage;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SendRedPacketResponse {
    private String sessionId;

    private Integer sessionType;

    private Integer type;

    private Long messageId;

    private Object body;

    private String createdAt;
}