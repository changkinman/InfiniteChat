package com.shanyangcode.userservice.model.dto.response;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 创建群聊响应DTO
 */
@Data
public class CreateGroupResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建者ID
     */
    private String creatorId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话名称（群名）
     */
    private String sessionName;

    /**
     * 会话类型 (1:群聊)
     */
    private Integer sessionType;

    /**
     * 群头像URL
     */
    private String avatar;

    /**
     * 邀请失败的成员ID列表
     */
    private List<String> failedMemberIds;
}