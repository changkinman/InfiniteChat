package com.shangyangcode.infinitechat.contactservice.data.DeleteFriend;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class DeleteFriendRequest {

    @NotNull(message = "发起用户 uuid 不能为空")
    private Long userUuid;

    @NotNull(message = "目标用户 uuid 不能为空")
    private Long receiveUserUuid;
}