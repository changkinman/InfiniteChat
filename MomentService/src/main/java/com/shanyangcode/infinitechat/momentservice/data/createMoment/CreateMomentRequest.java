package com.shanyangcode.infinitechat.momentservice.data.createMoment;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Accessors(chain = true)
public class CreateMomentRequest {

    @NotEmpty(message = "用户ID不能为空")
    private String userId;

    private String text;

    private List<String> mediaUrls;
}