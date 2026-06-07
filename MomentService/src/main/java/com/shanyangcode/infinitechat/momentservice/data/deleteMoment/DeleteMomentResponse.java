package com.shanyangcode.infinitechat.momentservice.data.deleteMoment;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeleteMomentResponse {
    /**
     * 操作结果消息
     */
    private String message;
}