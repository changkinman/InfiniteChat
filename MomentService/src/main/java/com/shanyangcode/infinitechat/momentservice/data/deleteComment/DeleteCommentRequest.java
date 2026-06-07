package com.shanyangcode.infinitechat.momentservice.data.deleteComment;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class DeleteCommentRequest {
    /**
     * 朋友圈ID (路径参数)
     */
    @NotNull(message = "朋友圈ID不能为空")
    private Long momentId;

    /**
     * 评论ID (查询参数)
     */
    @NotNull(message = "评论ID不能为空")
    private Long commentId;

    /**
     * 用户ID (操作者) (查询参数)
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}