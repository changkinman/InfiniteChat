package com.shanyangcode.userservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.userservice.constants.FriendStatusEnum;
import com.shanyangcode.userservice.mapper.FriendMapper;
import com.shanyangcode.userservice.mapper.SessionMapper;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.dto.NewGroupSessionNotificationDTO;
import com.shanyangcode.userservice.model.dto.request.CreateGroupRequest;
import com.shanyangcode.userservice.model.dto.response.CreateGroupResponse;
import com.shanyangcode.userservice.model.entity.Friend;
import com.shanyangcode.userservice.model.entity.Session;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.service.NotificationService;
import com.shanyangcode.userservice.service.SessionService;
import com.shanyangcode.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 *
 * 核心改动：
 * - 使用Lambda Wrapper替代string-based查询
 * - 使用Kafka异步通知替代HTTP同步调用
 * - 使用SnowflakeUtil替代Snowflake
 */
@Slf4j
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session>
        implements SessionService {

    private final UserService userService;
    private final FriendMapper friendMapper;
    private final UserSessionMapper userSessionMapper;
    private final SessionMapper sessionMapper;
    private final NotificationService notificationService;

    /**
     * 用户角色常量
     */
    private static final int USER_ROLE_GROUP_OWNER = 0; // 群主
    private static final int USER_ROLE_GROUP_MEMBER = 2; // 群成员

    /**
     * 会话状态常量
     */
    private static final int SESSION_STATUS_NORMAL = 0;

    /**
     * 用户状态常量
     */
    private static final int USER_STATUS_NORMAL = 0;

    /**
     * 默认群头像URL
     */
    private static final String DEFAULT_GROUP_AVATAR_URL = "https://video.shanyangcode.com/image/default/A9C9C83CCCE043EC8253DB5D7545DCB4-6-2.png";

    public SessionServiceImpl(UserService userService,
                              FriendMapper friendMapper,
                              UserSessionMapper userSessionMapper,
                              SessionMapper sessionMapper,
                              NotificationService notificationService) {
        this.userService = userService;
        this.friendMapper = friendMapper;
        this.userSessionMapper = userSessionMapper;
        this.sessionMapper = sessionMapper;
        this.notificationService = notificationService;
    }

    /**
     * 创建群聊
     *
     * @param request 群聊创建请求参数
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        Long creatorId = request.getCreatorId();
        List<Long> memberIds = request.getMemberIds();
        List<String> failedMemberIds = new ArrayList<>();

        // 参数校验
        validateCreateGroupParameters(creatorId, memberIds);

        // 确认创建者用户存在且状态正常
        getActiveUserById(creatorId);

        // 验证好友关系并获取有效成员ID
        List<Long> validMemberIds = validateAndFilterMembers(creatorId, memberIds, failedMemberIds);

        ThrowUtils.throwIf(validMemberIds.isEmpty(), ErrorCode.OPERATION_ERROR, "没有有效的好友可加入群聊");

        // 生成sessionId
        Long sessionId = SnowflakeUtil.nextId();

        // 生成群名称
        String groupName = generateGroupName(creatorId, validMemberIds);

        // 插入session表
        Session session = createSession(sessionId, groupName);
        sessionMapper.insert(session);

        // 插入user_session表 - 创建者
        insertUserSession(sessionId, creatorId, USER_ROLE_GROUP_OWNER);

        // 计算成员数量 = 有效成员数 + 群主
        int membersCount = validMemberIds.size() + 1;

        // 构建推送新群会话消息
        NewGroupSessionNotificationDTO notification = buildNewGroupSessionNotification(creatorId, groupName, membersCount);

        // 插入user_session表 - 其他成员并推送Kafka通知
        insertMembersAndPushNotifications(validMemberIds, sessionId, notification);

        // 响应结果
        CreateGroupResponse response = new CreateGroupResponse();
        BeanUtils.copyProperties(notification, response);
        response.setCreatorId(String.valueOf(creatorId));
        response.setSessionId(String.valueOf(sessionId));
        response.setSessionType(SessionTypeConstant.GROUP_TYPE);
        response.setFailedMemberIds(failedMemberIds);
        return response;
    }

    /* ===================== 私有方法 ===================== */

    /**
     * 校验创建群聊请求参数的合法性
     */
    private void validateCreateGroupParameters(Long creatorId, List<Long> memberIds) {
        ThrowUtils.throwIf(creatorId == null, ErrorCode.PARAMS_ERROR, "创建者ID不能为空");
        ThrowUtils.throwIf(memberIds == null || memberIds.isEmpty(), ErrorCode.PARAMS_ERROR, "成员ID列表不能为空");
    }

    /**
     * 判断用户状态信息
     */
    private void getActiveUserById(Long userId) {
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null || user.getState() != USER_STATUS_NORMAL,
                ErrorCode.NOT_FOUND_ERROR, "用户不存在或状态异常");
    }

    /**
     * 验证并过滤成员ID，返回有效的成员ID列表
     */
    private List<Long> validateAndFilterMembers(Long creatorId, List<Long> memberIds, List<String> failedMemberIds) {
        // 获取创建者所有好友ID
        LambdaQueryWrapper<Friend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(Friend::getUserId, creatorId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode());
        List<Friend> friends = friendMapper.selectList(friendWrapper);

        Set<Long> friendIdSet = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toSet());

        List<Long> validMemberIds = new ArrayList<>();

        for (Long memberId : memberIds) {
            if (friendIdSet.contains(memberId)) {
                validMemberIds.add(memberId);
            } else {
                failedMemberIds.add(String.valueOf(memberId));
                log.info("成员ID {} 不是创建者的好友，无法加入群聊", memberId);
            }
        }

        return validMemberIds;
    }

    /**
     * 生成群名称，最多16个字符
     */
    private String generateGroupName(Long creatorId, List<Long> memberIds) {
        StringBuilder groupNameBuilder = new StringBuilder();
        List<Long> allMemberIds = new ArrayList<>(memberIds);
        allMemberIds.add(0, creatorId); // 确保群主 ID 在首位

        // 查询所有用户信息
        List<User> users = userService.listByIds(allMemberIds);

        // 构建 ID -> User 映射，确保顺序可控
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        // 按照 allMemberIds 的顺序拼接用户名
        for (Long memberId : allMemberIds) {
            User user = userMap.get(memberId);
            if (user != null) {
                if (!groupNameBuilder.isEmpty()) {
                    groupNameBuilder.append("、");
                }
                groupNameBuilder.append(user.getNickname());
                if (groupNameBuilder.length() >= 16) {
                    groupNameBuilder.setLength(16); // 截取前 16 个字符
                    break;
                }
            }
        }
        return groupNameBuilder.toString();
    }

    /**
     * 创建会话对象
     */
    private Session createSession(Long sessionId, String groupName) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setName(groupName);
        session.setType(SessionTypeConstant.GROUP_TYPE);
        session.setStatus(SESSION_STATUS_NORMAL);
        session.setAvatar(DEFAULT_GROUP_AVATAR_URL);
        session.setCreatedTime(new Date());
        session.setUpdatedTime(new Date());
        return session;
    }

    /**
     * 插入用户会话关系
     */
    private void insertUserSession(Long sessionId, Long userId, int role) {
        UserSession userSession = new UserSession();
        userSession.setUserId(userId);
        userSession.setSessionId(sessionId);
        userSession.setRole(role);
        userSession.setStatus(SESSION_STATUS_NORMAL);
        userSession.setCreatedTime(new Date());
        userSession.setUpdatedTime(new Date());
        userSessionMapper.insert(userSession);
    }

    /**
     * 构建新群会话的通知消息
     */
    private NewGroupSessionNotificationDTO buildNewGroupSessionNotification(Long creatorId, String groupName, int membersCount) {
        NewGroupSessionNotificationDTO notification = new NewGroupSessionNotificationDTO();
        notification.setSessionName(groupName);
        notification.setAvatar(DEFAULT_GROUP_AVATAR_URL);
        notification.setCreatorId(creatorId);
        notification.setMembersCount(membersCount);
        return notification;
    }

    /**
     * 插入成员并推送Kafka通知
     */
    private void insertMembersAndPushNotifications(List<Long> memberIds, Long sessionId,
                                                   NewGroupSessionNotificationDTO notification) {
        for (Long memberId : memberIds) {
            // 插入用户会话关系
            insertUserSession(sessionId, memberId, USER_ROLE_GROUP_MEMBER);

            // 推送Kafka通知
            try {
                notificationService.pushGroupNewSession(memberId, sessionId, notification);
            } catch (Exception e) {
                log.error("推送群聊会话失败，成员ID {}，错误信息：{}", memberId, e.getMessage());
            }
        }
    }
}