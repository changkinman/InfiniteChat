package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.common.enums.UserSessionStatusEnum;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.service.UserSessionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession>
    implements UserSessionService {


    @Override
    public List<Long> getUserIdBySessionId(Long sessionId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId);
        List<UserSession> userSessions = this.list(queryWrapper);
        return userSessions.stream().map(UserSession::getUserId).collect(Collectors.toList());
    }


    @Override
    public List<Long> getSessionIdsByUserId(Long userId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<UserSession> userSessions = this.list(queryWrapper);
        return userSessions.stream().map(UserSession::getSessionId).collect(Collectors.toList());
    }

    @Override
    public int getGroupMemberCount(Long sessionId) {
        LambdaQueryWrapper<UserSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSession::getSessionId, sessionId)
                .eq(UserSession::getStatus, UserSessionStatusEnum.NORMAL.getCode());
        return Math.toIntExact(this.count(wrapper));
    }
}