package com.shanyangcode.infinitechat.momentservice.data.deleteMoment;


import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class DeleteMomentRequest {
    /**
     * 朋友圈ID (路径参数)
     */
    @NotNull(message = "朋友圈ID不能为空")
    private Long momentId;

    /**
     * 用户ID (操作者) (查询参数)
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}