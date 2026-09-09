package com.shangyangcode.infinitechat.contactservice.data.BlockFriend;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class BlockFriendRequest {
    @NotNull(message = "发起用户 uuid 不能为空")
    private String userUuid;

    @NotNull(message = "目标用户 uuid 不能为空")
    private String receiveUserUuid;
}