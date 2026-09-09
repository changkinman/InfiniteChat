package com.shangyangcode.infinitechat.contactservice.data.AddFriend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class FriendApplicationNotification {

    private String applyUserName;
}
