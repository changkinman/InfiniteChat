package com.shanyangcode.infinitechat.momentservice.data.createLike;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class CreateLikeRequest {
    /**
     * 用户ID (点赞者) (查询参数)
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}