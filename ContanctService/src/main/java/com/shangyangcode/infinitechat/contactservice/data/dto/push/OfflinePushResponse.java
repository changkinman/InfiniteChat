package com.shangyangcode.infinitechat.contactservice.data.dto.push;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OfflinePushResponse {

    private List<NewSessionNotification> newSessionPushes;

    private List<NewGroupSessionNotification> newGroupPushes;

    private List<FriendApplicationNotification> friendRequests;
}
