package com.shangyangcode.infinitechat.contactservice.data.FriendDetail;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class FriendDetailRequest {

    @NotNull(message = "发起者 uuid 不能为空")
    private Long userUuid;

    @NotNull(message = "好友 uuid 不能为空")
    private Long friendUuid;
}