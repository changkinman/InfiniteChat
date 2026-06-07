package com.shanyangcode.infinitechat.momentservice.data.deleteComment;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeleteCommentResponse {
    /**
     * 操作结果消息
     */
    private String message;
}
