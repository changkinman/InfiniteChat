package com.shanyangcode.userservice.model.dto.response;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 群聊邀请响应DTO
 */
@Data
public class InviteGroupResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功邀请的用户ID列表
     */
    private List<String> successIds;

    /**
     * 邀请失败的用户ID列表
     */
    private List<String> failedIds;
}