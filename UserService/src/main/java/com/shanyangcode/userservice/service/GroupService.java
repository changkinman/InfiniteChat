package com.shanyangcode.userservice.service;

import com.shanyangcode.userservice.model.dto.request.InviteGroupRequest;
import com.shanyangcode.userservice.model.dto.response.InviteGroupResponse;

/**
 * 群组服务接口
 *
 * 功能说明：
 * - 处理群组邀请相关业务
 */
public interface GroupService {

    /**
     * 邀请用户加入群聊
     *
     * 处理流程：
     * 1. 验证会话存在且为群聊类型
     * 2. 验证邀请者权限（必须是群主或管理员）
     * 3. 验证被邀请者都是邀请者的好友
     * 4. 检查被邀请者是否已在群中
     * 5. 创建UserSession记录
     * 6. 发送Kafka通知
     *
     * @param request 邀请请求参数
     * @return 邀请结果（成功列表、失败列表）
     */
    InviteGroupResponse inviteGroup(InviteGroupRequest request);
}