package com.shangyangcode.infinitechat.contactservice.data.KickGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 响应 DTO，包含成功移出的用户 ID 列表。
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class KickGroupMembersResponse {

    /**
     * 成功移出的 userId 列表
     */
    private List<String> successIds;
}
