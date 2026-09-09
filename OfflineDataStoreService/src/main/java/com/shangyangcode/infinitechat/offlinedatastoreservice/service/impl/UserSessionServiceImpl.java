package com.shangyangcode.infinitechat.offlinedatastoreservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.offlinedatastoreservice.mapper.UserSessionMapper;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.UserSession;
import com.shangyangcode.infinitechat.offlinedatastoreservice.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession>
    implements UserSessionService {

    @Override
    public Set<Long> findSessionIdByUserId(Long userId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        List<UserSession> userSessions = this.baseMapper.selectList(queryWrapper);

        return userSessions.stream().map(UserSession::getSessionId).collect(Collectors.toSet());
    }

}

































