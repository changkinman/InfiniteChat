package com.shangyangcode.infinitechat.contactservice.data.UnreadApply;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class UnreadApplyRequest {

    @NotNull(message = "用户 uuid 不能为空")
    private Long userUuid;
}