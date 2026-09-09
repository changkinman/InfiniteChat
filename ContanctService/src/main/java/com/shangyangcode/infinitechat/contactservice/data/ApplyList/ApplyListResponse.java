package com.shangyangcode.infinitechat.contactservice.data.ApplyList;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ApplyListResponse {
    private long total;

    private List<applyFriend> data;
}