package com.shanyangcode.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.userservice.model.dto.request.CreateGroupRequest;
import com.shanyangcode.userservice.model.dto.response.CreateGroupResponse;
import com.shanyangcode.userservice.model.entity.Session;


public interface SessionService extends IService<Session> {

    /**
     * 创建群聊
     *
     * 处理流程：
     * 1. 验证创建者用户存在且状态正常
     * 2. 验证所有成员都是创建者的好友
     * 3. 生成群名称（成员昵称拼接，最多16字符）
     * 4. 创建Session记录
     * 5. 创建创建者的UserSession记录（角色：群主）
     * 6. 为所有成员创建UserSession记录（角色：普通成员）
     * 7. 发送Kafka通知给所有成员
     *
     * @param request 创建群聊请求参数
     * @return 创建结果（包含sessionId、群名、失败成员列表）
     */
    CreateGroupResponse createGroup(CreateGroupRequest request);

}