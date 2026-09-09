package com.shangyangcode.infinitechat.contactservice.data.ExitGroup;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class ExitGroupRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    /**
     * 用户ID
     */
    @NotNull(message = "userId不能为空")
    private Long userId;
}
