package com.shanyangcode.userservice.model.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 群聊邀请请求DTO
 */
@Data
public class InviteGroupRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /**
     * 邀请者用户ID
     */
    @NotNull(message = "邀请者ID不能为空")
    private Long inviterId;

    /**
     * 被邀请者用户ID列表
     */
    @NotNull(message = "被邀请者ID列表不能为空")
    private List<Long> inviteeIds;
}