package com.shanyangcode.infinitechat.messageingservice.data.sendMsg;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class KafkaMsgVO {

    protected Long sessionId;

    protected Long sendUserId;

    protected Long messageId;

    protected Integer sessionType;

    protected Integer type;

    protected String messageUuid;

    protected Date createAt;

    protected Object body;
}
