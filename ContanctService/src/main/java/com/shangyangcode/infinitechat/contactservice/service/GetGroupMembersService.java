package com.shangyangcode.infinitechat.contactservice.service;

import com.shangyangcode.infinitechat.contactservice.data.GetGroupMembers.GroupMembersRequest;
import com.shangyangcode.infinitechat.contactservice.data.GetGroupMembers.GroupMembersResponse;

/**
 * 群聊成员获取服务接口
 */
public interface GetGroupMembersService {

    GroupMembersResponse getGroupMembers(GroupMembersRequest request);
}