package com.shangyangcode.infinitechat.contactservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.contactservice.mapper.UserSessionMapper;
import com.shangyangcode.infinitechat.contactservice.model.UserSession;
import com.shangyangcode.infinitechat.contactservice.service.UserSessionService;
import org.springframework.stereotype.Service;

@Service
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements UserSessionService {
}