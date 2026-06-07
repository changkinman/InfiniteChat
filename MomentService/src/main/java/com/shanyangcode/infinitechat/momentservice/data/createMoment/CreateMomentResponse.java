package com.shanyangcode.infinitechat.momentservice.data.createMoment;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class CreateMomentResponse {
    private Long momentId;

    private Long userId;

    private String text;

    private List<String> mediaUrls;
}