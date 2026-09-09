package com.shangyangcode.infinitechat.contactservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shangyangcode.infinitechat.contactservice.data.CreateGroup.CreateGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.CreateGroup.CreateGroupResponse;
import com.shangyangcode.infinitechat.contactservice.model.Session;

public interface SessionService extends IService<Session> {
    CreateGroupResponse createGroup(CreateGroupRequest request);
}