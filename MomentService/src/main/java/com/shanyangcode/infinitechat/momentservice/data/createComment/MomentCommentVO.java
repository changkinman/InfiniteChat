package com.shanyangcode.infinitechat.momentservice.data.createComment;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MomentCommentVO {
    private Long parentCommentId;

    private String parentUserName;

    private Long commentId;

    private String userName;

    private String comment;
}