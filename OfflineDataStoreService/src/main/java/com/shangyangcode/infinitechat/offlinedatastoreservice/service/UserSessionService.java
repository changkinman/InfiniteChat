package com.shangyangcode.infinitechat.offlinedatastoreservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.UserSession;

import java.util.Set;

public interface UserSessionService extends IService<UserSession> {
     Set<Long> findSessionIdByUserId(Long userId);

}
