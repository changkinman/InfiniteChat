package com.shangyangcode.infinitechat.contactservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.contactservice.constants.ErrorEnum;
import com.shangyangcode.infinitechat.contactservice.data.KickGroup.KickGroupMembersRequest;
import com.shangyangcode.infinitechat.contactservice.data.KickGroup.KickGroupMembersResponse;
import com.shangyangcode.infinitechat.contactservice.exception.GroupException;
import com.shangyangcode.infinitechat.contactservice.exception.ServiceException;
import com.shangyangcode.infinitechat.contactservice.exception.UserException;
import com.shangyangcode.infinitechat.contactservice.mapper.SessionMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.UserMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.UserSessionMapper;
import com.shangyangcode.infinitechat.contactservice.model.Session;
import com.shangyangcode.infinitechat.contactservice.model.User;
import com.shangyangcode.infinitechat.contactservice.model.UserSession;
import com.shangyangcode.infinitechat.contactservice.service.KickGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群聊踢人业务逻辑
 */
@Service
@Slf4j
public class KickGroupServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements KickGroupService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private UserSessionMapper userSessionMapper;

    // 用户状态常量
    private static final int USER_STATUS_NORMAL = 1;

    // 会话类型常量
    private static final int SESSION_TYPE_GROUP = 2;

    // 用户会话角色常量
    private static final int ROLE_OWNER = 1;
    private static final int ROLE_ADMIN = 2;
    private static final int ROLE_MEMBER = 3;

    // 用户会话状态常量
    private static final int USER_SESSION_STATUS_NORMAL = 1;


    /**
     * 踢出群聊成员。
     *
     * @param request 包含 sessionId、operatorId 和 memberIds 的请求体
     * @return 成功移出群聊的用户 ID 列表
     */
    @Transactional
    @Override
    public KickGroupMembersResponse kickGroupMembers(KickGroupMembersRequest request) {
        String sessionId = request.getSessionId();
        Long operatorId = request.getOperatorId();
        List<Long> memberIds = request.getMemberIds();

        // 参数校验
        User operator = validateOperator(operatorId, sessionId);

        // 获取操作者在群聊中的角色
        UserSession operatorSession = getUserSession(operatorId, sessionId);
        boolean isOwner = operatorSession.getRole() == ROLE_OWNER;
        boolean isAdmin = operatorSession.getRole() == ROLE_ADMIN;

        // 校验并收集需要踢出的成员
        List<UserSession> membersToKick = validateAndCollectMembers(sessionId, memberIds, isOwner, isAdmin);

        // 执行踢人操作
        List<String> successIds = performKick(membersToKick);

        return new KickGroupMembersResponse(successIds);
    }

    private User validateOperator(Long operatorId, String sessionId) {
        User operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new UserException(ErrorEnum.NO_USER_ERROR);
        }
        if (operator.getStatus() != USER_STATUS_NORMAL) {
            throw new UserException(ErrorEnum.USER_STATUS_ERROR);
        }

        Session session = getSession(sessionId);

        // 确保会话类型为群聊
        if (session.getType() != SESSION_TYPE_GROUP) {
            throw new GroupException(ErrorEnum.NOT_GROUP);
        }

        // 检查操作者在群聊中的角色
        UserSession operatorSession = getUserSession(operatorId, sessionId);
        if (operatorSession == null) {
            throw new ServiceException("操作者不在该群聊中");
        }

        if (operatorSession.getRole() != ROLE_OWNER && operatorSession.getRole() != ROLE_ADMIN) {
            throw new ServiceException("操作者不是群主或管理员");
        }

        return operator;
    }

    /**
     * 根据 sessionId 获取会话信息。
     *
     * @param sessionId 群聊的 sessionId
     * @return 会话实体
     * @throws ServiceException 如果会话不存在或已删除
     */
    private Session getSession(String sessionId) {
        long sessionIdLong;
        try {
            sessionIdLong = Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            throw new ValidationException("无效的 sessionId 格式");
        }

        Session session = sessionMapper.selectById(sessionIdLong);
        if (session == null || session.getStatus() != 1) { // 假设 status=1 为正常
            throw new GroupException(ErrorEnum.GROUP_NOT_EXIST);
        }

        return session;
    }

    /**
     * 获取用户在特定会话中的 UserSession 信息。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return UserSession 实体
     */
    private UserSession getUserSession(Long userId, String sessionId) {
        long sessionIdLong;
        try {
            sessionIdLong = Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            throw new ValidationException("无效的 sessionId 格式");
        }

        QueryWrapper<UserSession> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("session_id", sessionIdLong).eq("status", USER_SESSION_STATUS_NORMAL);

        return userSessionMapper.selectOne(query);
    }


    /**
     * 校验每个被踢出成员的合法性，并收集对应的 UserSession 实体。
     *
     * @param sessionId 群聊的 sessionId
     * @param memberIds 被踢出者的 userId 列表
     * @param isOwner   操作者是否为群主
     * @param isAdmin   操作者是否为管理员
     * @return 需要踢出的成员的 UserSession 列表
     * @throws ServiceException 如果存在不合法的被踢出成员
     */
    private List<UserSession> validateAndCollectMembers(String sessionId, List<Long> memberIds, boolean isOwner, boolean isAdmin) {
        Long sessionIdLong = Long.parseLong(sessionId);

        // 批量查询被踢出成员的 UserSession
        QueryWrapper<UserSession> query = new QueryWrapper<>();
        query.eq("session_id", sessionIdLong).in("user_id", memberIds).eq("status", USER_SESSION_STATUS_NORMAL);

        List<UserSession> userSessions = userSessionMapper.selectList(query);

        // 检查所有 memberIds 是否都在群聊中
        Set<Long> existingMemberIds = userSessions.stream().map(UserSession::getUserId).collect(Collectors.toSet());
        List<Long> notInGroupIds = memberIds.stream().filter(id -> !existingMemberIds.contains(id)).collect(Collectors.toList());

        if (!notInGroupIds.isEmpty()) {
            throw new GroupException(ErrorEnum.KICKED_NOT_IN_GROUP.getCode(), ErrorEnum.KICKED_NOT_IN_GROUP.getMessage() + notInGroupIds);
        }

        // 校验每个被踢出成员的角色
        for (UserSession userSession : userSessions) {
            int role = userSession.getRole();
            if (role == ROLE_OWNER) {
                log.info("不能踢出群主: userId=" + userSession.getUserId());
                throw new GroupException(ErrorEnum.KICKED_IS_OWNER);
            }
            if (isAdmin && role == ROLE_ADMIN) {
                log.info("管理员不能踢出其他管理员: userId=" + userSession.getUserId());
                throw new GroupException(ErrorEnum.KICKED_IS_ADMIN);
            }
        }
        return userSessions;
    }

    /**
     * 执行踢人操作，通过批量删除 UserSession。
     *
     * @param membersToKick 需要踢出的成员的 UserSession 列表
     * @return 成功移出的用户 ID 列表
     * @throws ServiceException 如果没有任何用户被成功踢出
     */
    private List<String> performKick(List<UserSession> membersToKick) {
        if (membersToKick == null || membersToKick.isEmpty()) {
            throw new GroupException(ErrorEnum.KICKED_NO_USER);
        }

        // 提取需要删除的 session ID 列表
        List<Long> sessionIds = membersToKick.stream()
                .map(UserSession::getId)
                .collect(Collectors.toList());

        // 执行批量删除操作
        int deletedCount = userSessionMapper.deleteBatchIds(sessionIds);

        if (deletedCount <= 0) {
            throw new GroupException(ErrorEnum.KICKED_NO_SUCCESS);
        }

        // 获取踢出的用户ID列表
        return membersToKick.stream()
                .map(session -> String.valueOf(session.getUserId()))
                .collect(Collectors.toList());
    }


}

