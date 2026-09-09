package com.shangyangcode.infinitechat.contactservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.contactservice.constants.*;
import com.shangyangcode.infinitechat.contactservice.data.CreateGroup.CreateGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.CreateGroup.CreateGroupResponse;
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
import com.shangyangcode.infinitechat.contactservice.service.PushService;
import com.shangyangcode.infinitechat.contactservice.service.SessionService;
import com.shangyangcode.infinitechat.contactservice.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private UserSessionMapper userSessionMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private PushService pushService;

    @Autowired
    private UserSessionService userSessionService;


    /**
     * 处理群聊创建逻辑
     *
     * @param request 群聊创建请求参数
     * @return 创建结果
     * @throws ServiceException 业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateGroupResponse createGroup(CreateGroupRequest request) throws ServiceException {
        Long creatorId = request.getCreatorId();
        List<Long> memberIds = request.getMemberIds();
        List<String> failedMemberIds = new ArrayList<>();

        // 确认创建者用户存在且状态正常
        getActiveUserById(creatorId);

        // 生成sessionId
        Long sessionId = generateId();

        // 生成群名称
        String groupName = generateGroupName(creatorId, memberIds);

        // 插入session表
        createSession(sessionId, groupName);

        // 插入user_session表 - 创建者
        insertUserSession(sessionId, creatorId);

        // 构建推送新群会话消息
        NewGroupSessionNotification notification = buildNewGroupSessionNotification(creatorId, sessionId, groupName);

        // 插入user_session表 - 其他成员并推送通知
        insertMembersAndPushNotifications(memberIds, sessionId, notification, failedMemberIds);

        // 响应结果
        CreateGroupResponse response = new CreateGroupResponse();
        BeanUtils.copyProperties(notification, response);
        response.setFailedMemberIds(failedMemberIds);
        return response;
    }

    /**
     * 获取活跃用户信息
     *
     * @param userId 用户ID
     * @return 活跃用户对象
     * @throws ServiceException 用户不存在或状态不正常
     */
    private User getActiveUserById(Long userId) throws ServiceException {
        User user = userMapper.selectById(userId);
        // 判断用户是否为空 和 状态是否异常
        if (user == null) {
            throw new UserException(ErrorEnum.NO_USER_ERROR);
        }
        if (user.getStatus() != UserStatus.NORMAL.getValue()) {
            throw new UserException(ErrorEnum.USER_STATUS_ERROR);
        }
        return user;
    }

    /**
     * 生成唯一的sessionId
     *
     * @return sessionId
     */
    private Long generateId() {
        Snowflake snowflake = IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
        );
        return snowflake.nextId();
    }

    /**
     * 生成群名称，最多16个字符
     *
     * @param creatorId 创建者用户ID
     * @param memberIds 成员用户ID列表
     * @return 群名称
     */
    private String generateGroupName(Long creatorId, List<Long> memberIds) {
        StringBuilder groupNameBuilder = new StringBuilder();
        List<Long> allMemberIds = new ArrayList<>(memberIds);
        allMemberIds.add(0, creatorId); // 确保群主 ID 在首位

        // 查询所有用户信息
        List<User> users = userMapper.selectBatchIds(allMemberIds);

        // 构建 ID -> User 映射，确保顺序可控
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        // 按照 allMemberIds 的顺序拼接用户名
        for (Long memberId : allMemberIds) {
            User user = userMap.get(memberId);
            if (user != null) {
                if (groupNameBuilder.length() > 0) {
                    groupNameBuilder.append("、");
                }
                groupNameBuilder.append(user.getUserName());
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
     *
     * @param sessionId 会话ID
     * @param groupName 群名称
     * @return 会话对象
     */
    private void createSession(Long sessionId, String groupName) {
        sessionMapper.insert(new Session()
                .setId(sessionId)
                .setName(groupName)
                .setType(SessionType.GROUP.getValue())
                .setStatus(SessionStatus.NORMAL.getValue()));
    }

    /**
     * 插入用户会话关系
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    private void insertUserSession(Long sessionId, Long userId) {
        UserSession userSession = new UserSession()
                .setId(generateId())
                .setUserId(userId)
                .setSessionId(sessionId)
                .setRole(UserRole.GROUP_OWNER.getValue())
                .setStatus(UserStatus.NORMAL.getValue());
        userSessionMapper.insert(userSession);
    }

    /**
     * 构建新群会话的通知消息
     *
     * @param creatorId 创建者ID
     * @param sessionId 会话ID
     * @param groupName 群名称
     * @return 新群会话通知对象
     */
    private NewGroupSessionNotification buildNewGroupSessionNotification(Long creatorId, Long sessionId, String groupName) {
        return new NewGroupSessionNotification()
                .setCreatorId(String.valueOf(creatorId))
                .setSessionId(String.valueOf(sessionId))
                .setSessionName(groupName)
                .setSessionType(SessionType.GROUP.getValue())
                .setAvatar(ConfigEnum.GROUP_AVATAR_URL.getValue());
    }

    /**
     * 插入成员并推送通知
     *
     * @param memberIds       成员ID列表
     * @param sessionId       会话ID
     * @param notification    通知对象
     * @param failedMemberIds 邀请失败的成员ID列表
     */
    private void insertMembersAndPushNotifications(List<Long> memberIds, Long sessionId, NewGroupSessionNotification notification, List<String> failedMemberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        // 1. 批量构建UserSession对象
        List<UserSession> userSessions = new ArrayList<>(memberIds.size());

        for (Long memberId : memberIds) {
            userSessions.add(new UserSession()
                    .setId(generateId())
                    .setUserId(memberId)
                    .setSessionId(sessionId)
                    .setRole(UserRole.GROUP_MEMBER.getValue())
                    .setStatus(UserStatus.NORMAL.getValue()));
        }

        // 2. 使用MyBatis Plus的批量插入功能
        try {
            // 使用MyBatis Plus的批量保存方法
            userSessionService.saveBatch(userSessions);
        } catch (Exception e) {
            log.error("批量插入用户会话关系失败，sessionId: {}，错误：{}", sessionId, e.getMessage());
            throw new GroupException(ErrorEnum.GROUP_INSERT_ERROR);
        }

        // 3. 批量推送通知（保持原有的错误处理逻辑）
        for (Long memberId : memberIds) {
            try {
                pushService.pushGroupNewSession(memberId, notification);
            } catch (Exception e) {
                failedMemberIds.add(String.valueOf(memberId));
                log.error("推送群聊会话失败，成员ID {}，错误信息：{}", memberId, e.getMessage());
            }
        }
    }
}