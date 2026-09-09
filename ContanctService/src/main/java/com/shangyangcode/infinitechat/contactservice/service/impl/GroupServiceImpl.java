package com.shangyangcode.infinitechat.contactservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.contactservice.constants.*;
import com.shangyangcode.infinitechat.contactservice.data.InviteGroup.InviteGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.InviteGroup.InviteGroupResponse;
import com.shangyangcode.infinitechat.contactservice.data.dto.push.NewGroupSessionNotification;
import com.shangyangcode.infinitechat.contactservice.exception.GroupException;
import com.shangyangcode.infinitechat.contactservice.exception.ServiceException;
import com.shangyangcode.infinitechat.contactservice.exception.UserException;
import com.shangyangcode.infinitechat.contactservice.mapper.FriendMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.SessionMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.UserMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.UserSessionMapper;
import com.shangyangcode.infinitechat.contactservice.model.Friend;
import com.shangyangcode.infinitechat.contactservice.model.Session;
import com.shangyangcode.infinitechat.contactservice.model.User;
import com.shangyangcode.infinitechat.contactservice.model.UserSession;
import com.shangyangcode.infinitechat.contactservice.service.GroupService;
import com.shangyangcode.infinitechat.contactservice.service.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 群聊邀请服务实现类
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements GroupService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private FriendMapper friendMapper;


    @Autowired
    private UserSessionMapper userSessionMapper;

    @Autowired
    private PushService pushService;

    /**
     * 处理群聊邀请逻辑
     *
     * @param request 群聊邀请请求参数
     * @return 邀请结果
     */
    @Transactional
    @Override
    public InviteGroupResponse inviteGroup(InviteGroupRequest request) {
        Long sessionId = request.getSessionId();
        Long inviterId = request.getInviterId();
        List<Long> inviteeIds = request.getInviteeIds();

        Session session = sessionMapper.selectById(sessionId);

        // 参数校验
        validateParameters(session, inviterId, inviteeIds);

        // 验证好友关系并获取非好友ID
        Set<Long> nonFriendIds = getNonFriendIds(inviterId, inviteeIds);

        // 获取已在群聊中的用户ID
        Set<Long> alreadyInGroupIds = getAlreadyInGroupIds(sessionId, inviteeIds);

        // 准备成功和失败列表
        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        // 构建推送新群会话消息
        NewGroupSessionNotification notification = buildNewGroupSessionNotification(inviterId, sessionId, session);

        // 插入新成员并推送通知
        processInvitees(inviteeIds, alreadyInGroupIds, nonFriendIds, sessionId, successIds, failedIds, notification);

        return new InviteGroupResponse(successIds, failedIds);
    }

    /**
     * 校验请求参数的合法性
     *
     * @param session    群聊会话
     * @param inviterId  邀请者用户ID
     * @param inviteeIds 被邀请者用户ID列表
     * @throws ServiceException 参数校验失败时抛出异常
     */
    private void validateParameters(Session session, Long inviterId, List<Long> inviteeIds) throws ServiceException {
        // 校验群聊会话是否存在且类型为群聊
        if (session == null || session.getStatus() != UserStatus.NORMAL.getValue()
                || session.getType() != SessionType.GROUP.getValue()) {
            throw new ServiceException("群聊会话不存在或状态不正常");
        }

        // 校验邀请者是否存在且状态正常
        User inviter = userMapper.selectById(inviterId);
        if (inviter == null) {
            throw new UserException(ErrorEnum.NO_USER_ERROR);
        }
        if (inviter.getStatus() != UserStatus.NORMAL.getValue()) {
            throw new UserException(ErrorEnum.USER_STATUS_ERROR);
        }

        // 校验邀请者在群聊中的角色是否为群主或管理员
        UserSession inviterSession = getUserSession(inviterId, session.getId());
        if (inviterSession == null) {
            throw new GroupException(ErrorEnum.GROUP_INVITER_NOT_IN_GROUP);
        }

        if (!isInviterHasPermission(inviterSession.getRole())) {
            throw new GroupException(ErrorEnum.GROUP_INVITER_NO_PERMISSION);
        }

        // 校验被邀请者列表不能为空
        if (inviteeIds == null || inviteeIds.isEmpty()) {
            throw new GroupException(ErrorEnum.GROUP_INVITEE_LIST_EMPTY);
        }
    }

    /**
     * 获取邀请者在群聊中的会话信息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 用户会话信息
     */
    private UserSession getUserSession(Long userId, Long sessionId) {
        QueryWrapper<UserSession> query = new QueryWrapper<>();
        query.eq("user_id", userId)
                .eq("session_id", sessionId)
                .eq("status", UserStatus.NORMAL.getValue());
        return userSessionMapper.selectOne(query);
    }

    /**
     * 判断邀请者是否具有邀请权限
     *
     * @param role 用户角色
     * @return 是否具有权限
     */
    private boolean isInviterHasPermission(int role) {
        return role == UserRole.GROUP_OWNER.getValue() || role == UserRole.GROUP_ADMIN.getValue();
    }

    /**
     * 获取非好友的用户ID集合
     *
     * @param inviterId  邀请者用户ID
     * @param inviteeIds 被邀请者用户ID列表
     * @return 非好友的用户ID集合
     */
    private Set<Long> getNonFriendIds(Long inviterId, List<Long> inviteeIds) {
        // 如果inviteeIds为空，直接返回空集合
        if (inviteeIds == null || inviteeIds.isEmpty()) {
            return new HashSet<>();
        }

        // 1. 批量查询所有正常用户
        List<User> activeUsers = userMapper.selectList(
                new QueryWrapper<User>()
                        .in("user_id", inviteeIds)
                        .eq("status", UserStatus.NORMAL.getValue())
        );

        // 提取正常用户ID集合
        Set<Long> activeUserIds = activeUsers.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());

        // 2. 批量查询所有好友关系
        List<Friend> friends = friendMapper.selectList(
                new QueryWrapper<Friend>()
                        .eq("user_id", inviterId)
                        .in("friend_id", inviteeIds)
                        .eq("status", FriendStatus.NORMAL.getValue())
        );

        // 提取好友ID集合
        Set<Long> friendIds = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toSet());

        // 3. 获取非好友ID集合 (非正常用户 + 非好友)
        Set<Long> nonFriendIds = new HashSet<>();

        for (Long inviteeId : inviteeIds) {
            // 如果用户非正常
            if (!activeUserIds.contains(inviteeId)) {
                nonFriendIds.add(inviteeId);
                log.info("被邀请者ID {} 不存在或状态不正常", inviteeId);
                continue;
            }

            // 如果不是好友
            if (!friendIds.contains(inviteeId)) {
                nonFriendIds.add(inviteeId);
                log.info("邀请者ID {} 与被邀请者ID {} 不是好友", inviterId, inviteeId);
            }
        }

        return nonFriendIds;
    }


    /**
     * 获取已在群聊中的用户ID集合
     *
     * @param sessionId  群聊会话ID
     * @param inviteeIds 被邀请者用户ID列表
     * @return 已经在群聊中的用户ID集合
     */
    private Set<Long> getAlreadyInGroupIds(Long sessionId, List<Long> inviteeIds) {
        QueryWrapper<UserSession> query = new QueryWrapper<>();
        query.select("user_id")
                .eq("session_id", sessionId)
                .in("user_id", inviteeIds)
                .eq("status", UserStatus.NORMAL.getValue());

        List<UserSession> existingMembers = userSessionMapper.selectList(query);
        Set<Long> alreadyInGroupIds = new HashSet<>();
        for (UserSession member : existingMembers) {
            alreadyInGroupIds.add(member.getUserId());
        }
        return alreadyInGroupIds;
    }

    /**
     * 构建新群会话的通知消息
     *
     * @param inviterId 邀请者ID
     * @param sessionId 会话ID
     * @param session   会话信息
     * @return 新群会话通知对象
     */
    private NewGroupSessionNotification buildNewGroupSessionNotification(Long inviterId, Long sessionId, Session session) {
        String inviterIdStr = String.valueOf(inviterId);
        String sessionIdStr = String.valueOf(sessionId);
        String sessionName = session.getName();
        String sessionAvatarUrl = ConfigEnum.GROUP_AVATAR_URL.getValue();

        return new NewGroupSessionNotification(
                inviterIdStr,
                sessionIdStr,
                SessionType.GROUP.getValue(),
                sessionName,
                sessionAvatarUrl
        );
    }

    /**
     * 处理被邀请者列表，插入新成员并推送通知
     *
     * @param inviteeIds        被邀请者ID列表
     * @param alreadyInGroupIds 已在群聊中的用户ID集合
     * @param nonFriendIds      非好友的用户ID集合
     * @param sessionId         会话ID
     * @param successIds        成功邀请的用户ID列表
     * @param failedIds         邀请失败的用户ID列表
     * @param notification      新群会话通知
     */
    private void processInvitees(List<Long> inviteeIds,
                                 Set<Long> alreadyInGroupIds,
                                 Set<Long> nonFriendIds,
                                 Long sessionId,
                                 List<Long> successIds,
                                 List<Long> failedIds,
                                 NewGroupSessionNotification notification) {
        // 准备批处理所需的集合
        List<UserSession> validUserSessions = new ArrayList<>();
        Map<Long, UserSession> validInviteeMap = new HashMap<>();

        // 第一步：过滤无效邀请者并准备有效的UserSession对象
        for (Long inviteeId : inviteeIds) {
            // 跳过已在群组中的用户
            if (alreadyInGroupIds.contains(inviteeId)) {
                failedIds.add(inviteeId);
                log.info("用户ID {} 已在群聊中", inviteeId);
                continue;
            }

            // 跳过与邀请者不是好友或状态不正常的用户
            if (nonFriendIds.contains(inviteeId)) {
                failedIds.add(inviteeId);
                log.info("用户ID {} 与邀请者非好友关系或状态不正常", inviteeId);
                continue;
            }

            // 为有效邀请者创建UserSession
            UserSession userSession = createUserSession(inviteeId, sessionId);
            validUserSessions.add(userSession);
            validInviteeMap.put(inviteeId, userSession);
        }

        // 第二步：如果有有效用户要添加，执行批量插入
        if (!validUserSessions.isEmpty()) {
            try {
                // 使用MyBatis Plus批量插入以提高性能
                boolean batchResult = this.saveBatch(validUserSessions);

                if (batchResult) {
                    // 处理成功插入的用户
                    for (Long inviteeId : validInviteeMap.keySet()) {
                        successIds.add(inviteeId);
                        try {
                            // 向每个成功邀请的用户推送通知
                            pushService.pushGroupNewSession(inviteeId, notification);
                        } catch (Exception e) {
                            // 记录推送错误，但仍将用户视为添加成功
                            log.error("向用户ID {} 推送群聊通知失败", inviteeId, e);
                        }
                    }
                } else {
                    // 处理批量插入失败的情况
                    failedIds.addAll(validInviteeMap.keySet());
                    log.error("批量插入用户到群聊失败, 受影响用户数: {}", validInviteeMap.size());
                }
            } catch (Exception e) {
                // 处理批量插入异常
                failedIds.addAll(validInviteeMap.keySet());
                log.error("批量插入用户到群聊异常, 受影响用户数: {}", validInviteeMap.size(), e);
                throw e;
            }
        }
    }

    /**
     * 创建用户会话对象
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 用户会话对象
     */
    private UserSession createUserSession(Long userId, Long sessionId) {
        Date now = new Date();
        return new UserSession()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setRole(UserRole.GROUP_MEMBER.getValue())
                .setStatus(UserStatus.NORMAL.getValue())
                .setCreatedAt(now)
                .setUpdatedAt(now);
    }
}