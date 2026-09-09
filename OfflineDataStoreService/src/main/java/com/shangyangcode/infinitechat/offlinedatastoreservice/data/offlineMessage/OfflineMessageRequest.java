package com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class OfflineMessageRequest {
    @NotEmpty(message = "用户 ID 不能为空")
    private Long userId;

    @NotEmpty(message = "时间不能为空")
    private String time;
}