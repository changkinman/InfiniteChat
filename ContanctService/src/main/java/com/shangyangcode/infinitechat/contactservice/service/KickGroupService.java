package com.shangyangcode.infinitechat.contactservice.service;

import com.shangyangcode.infinitechat.contactservice.data.KickGroup.KickGroupMembersRequest;
import com.shangyangcode.infinitechat.contactservice.data.KickGroup.KickGroupMembersResponse;

/**
 * 群聊踢人服务接口
 */
public interface KickGroupService {

    /**
     * 踢出群聊成员
     *
     * @param request 包含 sessionId、operatorId 和 memberIds 的请求体
     * @return 成功移出群聊的用户 ID 列表
     */
    KickGroupMembersResponse kickGroupMembers(KickGroupMembersRequest request);
}