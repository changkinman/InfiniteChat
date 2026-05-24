package com.shanyangcode.infinitechat.authenticationservice.data.common.uploadUrl;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class UploadUrlRequest {
    @NotEmpty(message = "文件名称不能为空")
    private String fileName;
}
