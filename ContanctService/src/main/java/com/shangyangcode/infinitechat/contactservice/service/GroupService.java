package com.shangyangcode.infinitechat.contactservice.service;

import com.shangyangcode.infinitechat.contactservice.data.InviteGroup.InviteGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.InviteGroup.InviteGroupResponse;

/**
 * 群聊邀请服务接口
 */
public interface GroupService {

    /**
     * 处理群聊邀请逻辑
     *
     * @param request 群聊邀请请求参数
     * @return 邀请结果
     * @throws Exception 业务异常
     */
    InviteGroupResponse inviteGroup(InviteGroupRequest request) throws Exception;
}