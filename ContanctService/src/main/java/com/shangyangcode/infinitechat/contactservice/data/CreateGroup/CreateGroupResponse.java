package com.shangyangcode.infinitechat.contactservice.data.CreateGroup;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class CreateGroupResponse {

    private String sessionId;

    private String sessionName;

    private Integer sessionType;

    private String avatar;

    private String creatorId;

    private List<String> failedMemberIds;
}
