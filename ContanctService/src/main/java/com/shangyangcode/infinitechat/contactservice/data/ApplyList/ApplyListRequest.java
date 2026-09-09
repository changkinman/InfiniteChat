package com.shangyangcode.infinitechat.contactservice.data.ApplyList;

import lombok.Data;
import lombok.experimental.Accessors;
import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class ApplyListRequest {
    @NotNull(message = "用户 uuid 不能为空")
    private Long userUuid;

    private int pageNum;

    private int pageSize;

    private String key;
}