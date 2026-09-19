package com.shanyangcode.userservice.model.dto.request;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建群聊请求DTO
 */
@Data
public class CreateGroupRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建者用户ID
     */
    @NotNull(message = "创建者ID不能为空")
    private Long creatorId;

    /**
     * 成员用户ID列表
     */
    @NotNull(message = "成员ID列表不能为空")
    private List<Long> memberIds;
}