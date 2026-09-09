package com.shangyangcode.infinitechat.contactservice.service;

import com.shangyangcode.infinitechat.contactservice.data.ExitGroup.ExitGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.ExitGroup.ExitGroupResponse;

/**
 * 退出群聊服务接口
 */
public interface ExitGroupService {
    ExitGroupResponse exitGroup(ExitGroupRequest request);
}