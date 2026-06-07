package com.shanyangcode.infinitechat.momentservice.data.deleteLike;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeleteLikeResponse {
    /**
     * 操作结果消息
     */
    private String message;
}