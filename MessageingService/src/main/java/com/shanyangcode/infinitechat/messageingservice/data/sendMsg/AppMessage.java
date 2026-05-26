package com.shanyangcode.infinitechat.messageingservice.data.sendMsg;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class AppMessage {

    protected Long sessionId;

    protected List<Long> receiveUserIds;

    protected Long sendUserId;

    protected String userName;

    protected String avatar;

    protected Integer type;

    protected Long messageId;

    protected Integer sessionType;

    protected String sessionName;

    protected String sessionAvatar;

    private String createdAt;

    protected Object body;
}
