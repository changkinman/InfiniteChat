package com.shangyangcode.infinitechat.contactservice.data.ModifyApply;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ModifyApplyResponse {
    private String userId;

    private String sessionId;

    // 1 单聊，2 群聊
    private Integer sessionType;

    private String sessionName;

    private String avatar;
}