package com.shanyangcode.infinitechat.momentservice.data.createComment;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class MomentCommentDTO {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "评论内容不能为空")
    private String comment;

    private Long parentCommentId;
}