package com.shangyangcode.infinitechat.contactservice.data.KickGroup;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 请求 DTO，用于踢出群聊成员。
 */
@Data
@Accessors(chain = true)
public class KickGroupMembersRequest {

    /**
     * 群聊的 sessionId
     */
    @NotNull(message = "群聊id不能为空")
    private String sessionId;

    /**
     * 操作者的 userId
     */
    @NotNull(message = "操作者id不能为空")
    private Long operatorId;

    /**
     * 被移出者的 userId 列表
     */
    @NotNull(message = "被移出者不能为空")
    private List<Long> memberIds;
}
