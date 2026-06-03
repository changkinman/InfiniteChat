package com.shanyangcode.infinitechat.offlinedatastoreservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.offlinedatastoreservice.model.UserSession;

import java.util.Set;

public interface UserSessionService extends IService<UserSession> {
     Set<Long> findSessionIdByUserId(Long userId);

}
