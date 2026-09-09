package com.shangyangcode.infinitechat.contactservice.data.dto.push;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class NewGroupSessionNotification {

    private String creatorId;

    private String sessionId;

    // 1 单聊，2 群聊
    private Integer sessionType;

    private String sessionName;

    private String avatar;
}
