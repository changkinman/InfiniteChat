package com.shangyangcode.infinitechat.contactservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.shangyangcode.infinitechat.contactservice.constants.ConfigEnum;
import com.shangyangcode.infinitechat.contactservice.constants.FriendServiceConstants;
import com.shangyangcode.infinitechat.contactservice.constants.SessionType;
import com.shangyangcode.infinitechat.contactservice.data.BlockFriend.BlockFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.BlockFriend.BlockFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.DeleteFriend.DeleteFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.DeleteFriend.DeleteFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.FriendDetail.FriendDetailRequest;
import com.shangyangcode.infinitechat.contactservice.data.FriendDetail.FriendDetailResponse;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyResponse;
import com.shangyangcode.infinitechat.contactservice.data.SearchUser.SearchUserRequest;
import com.shangyangcode.infinitechat.contactservice.data.SearchUser.SearchUserResponse;
import com.shangyangcode.infinitechat.contactservice.data.dto.push.NewSessionNotification;
import com.shangyangcode.infinitechat.contactservice.exception.ServiceException;
import com.shangyangcode.infinitechat.contactservice.mapper.ApplyFriendMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.FriendMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.SessionMapper;
import com.shangyangcode.infinitechat.contactservice.mapper.UserSessionMapper;
import com.shangyangcode.infinitechat.contactservice.model.*;
import com.shangyangcode.infinitechat.contactservice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {
    @Autowired
    private UserService userService;

    @Autowired
    private UserSessionMapper userSessionMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ApplyFriendMapper applyFriendMapper;

    @Autowired
    private PushService pushService;

    private final Snowflake snowflake = IdUtil.getSnowflake(
            Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
            Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
    );

    @Override
    public SearchUserResponse searchUser(SearchUserRequest request) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", request.getPhone());

        User user = userService.getOne(queryWrapper);
        if (user == null) {
            throw new ServiceException(FriendServiceConstants.USER_NOT_EXIST);
        }

        validateFriendUser(user);

        SearchUserResponse response = new SearchUserResponse()
            .setUserUuid(String.valueOf(user.getUserId()))
            .setNickname(user.getUserName())
            .setAvatar(user.getAvatar())
            .setEmail(user.getEmail())
            .setPhone(user.getPhone())
            .setSignature(user.getSignature())
            .setGender(user.getGender());

        populateSessionId(Long.valueOf(request.getUserUuid()), user.getUserId(), response);
        response.setStatus(populateFriendStatus(Long.valueOf(request.getUserUuid()), user.getUserId()));

        return response;
    }

    private void validateFriendUser(User friendUser) {
        switch (friendUser.getStatus()) {
            case FriendServiceConstants.USER_STATUS_BANNED:
                throw new ServiceException(FriendServiceConstants.USER_BANNED);
            case FriendServiceConstants.USER_STATUS_DELETED:
                throw new ServiceException(FriendServiceConstants.USER_DELETED);
            default:
                break;
        }
    }

    private void populateSessionId(Long userId, Long friendId, SearchUserResponse response) {
        List<Long> commonSessionIds = userSessionMapper.findCommonSingleChatSessionIds(userId, friendId);
        if (commonSessionIds == null || commonSessionIds.isEmpty()) {
            response.setSessionId(null);
        } else {
            response.setSessionId(String.valueOf(commonSessionIds.get(0)));
        }
    }

    private int populateFriendStatus(Long userId, Long friendId) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("friend_id", friendId)
                .eq("user_id", userId);
        Friend friend = this.getOne(wrapper);
        if (friend != null) {
            return friend.getStatus();
        } else {
            return FriendServiceConstants.FRIEND_STATUS_NON_FRIEND;
        }
    }


    @Override
    @Transactional
    public DeleteFriendResponse deleteFriend(DeleteFriendRequest request) {
        Long userId = request.getUserUuid();
        Long friendId = request.getReceiveUserUuid();

        deleteApplyFriendRecords(userId, friendId);
        deleteFriendRecords(userId, friendId);
        deleteSessionRecords(userId, friendId);

        return new DeleteFriendResponse().setMessage("删除好友成功");
    }

    private void deleteApplyFriendRecords(Long userId, Long friendId) {
        QueryWrapper<ApplyFriend> applyFriendWrapper = new QueryWrapper<>();
        applyFriendWrapper
                .nested(wrapper -> wrapper.eq("user_id", userId).eq("target_id", friendId))
                .or()
                .nested(wrapper -> wrapper.eq("user_id", friendId).eq("target_id", userId));


        applyFriendMapper.delete(applyFriendWrapper);
    }

    private void deleteFriendRecords(Long userId, Long friendId) {
        QueryWrapper<Friend> friendWrapper = new QueryWrapper<>();
        friendWrapper
                .nested(wrapper -> wrapper.eq("user_id", userId).eq("friend_id", friendId))
                .or()
                .nested(wrapper -> wrapper.eq("user_id", friendId).eq("friend_id", userId));

        this.remove(friendWrapper);
    }

//    @Select("SELECT s.id " +
//            "FROM session s " +
//            "JOIN user_session us1 ON s.id = us1.session_id AND us1.user_id = #{userId} " +
//            "JOIN user_session us2 ON s.id = us2.session_id AND us2.user_id = #{friendId} " +
//            "WHERE s.type = 1 AND s.status != 2")

    private void deleteSessionRecords(Long userId, Long friendId) {
        MPJLambdaWrapper<Session> wrapper = new MPJLambdaWrapper<Session>()
                .select(Session::getId)
                .eq(Session::getType, 1)
                .ne(Session::getStatus, 2)
                .leftJoin(UserSession.class, UserSession::getSessionId, Session::getId)
                .leftJoin(UserSession.class, UserSession::getSessionId, Session::getId)
                .eq("t1.user_id", userId)
                .eq("t2.user_id", friendId);

        List<Session> sessions = sessionMapper.selectJoinList(Session.class, wrapper);
        ArrayList<Long> sessionIdList = new ArrayList<>();

        for(Session session: sessions){
            sessionIdList.add(session.getId());
        }

        if(!sessions.isEmpty()){
            QueryWrapper<UserSession> userSessionQueryWrapper = new QueryWrapper<>();
            userSessionQueryWrapper.in("session_id", sessionIdList);

            userSessionMapper.delete(userSessionQueryWrapper);
            sessionMapper.deleteBatchIds(sessionIdList);
        }
    }


    @Override
    @Transactional
    public BlockFriendResponse blockFriend(BlockFriendRequest request) {
        QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
        friendQueryWrapper
                .eq("user_id", request.getUserUuid())
                .eq("friend_id", request.getReceiveUserUuid());

        Friend friend = this.getOne(friendQueryWrapper);
        if (friend == null){
            throw new ServiceException(FriendServiceConstants.FRIEND_NOT_EXIST);
        }

        friend.setStatus(FriendServiceConstants.FRIEND_STATUS_BLOCKED);
        this.updateById(friend);

        return new BlockFriendResponse().setMessage("拉黑好友成功");
    }


    @Override
    public ModifyApplyResponse addFriend(Long userId, Long friendId) throws Exception {
        User user = userService.getById(userId);
        User applicant = userService.getById(friendId);

        createFriendRelations(userId, friendId);
        Long sessionId = createSession(userId, friendId);
        createUserSessions(userId, friendId, sessionId);
        sendPushNotification(user, friendId, sessionId);

        return buildModifyFriendApplicationResponse(applicant, sessionId);
    }

    private void createFriendRelations(Long userId, Long friendId) {
        Friend friend1 = new Friend();
        friend1.setId(snowflake.nextId());
        friend1.setUserId(userId);
        friend1.setFriendId(friendId);
        friend1.setStatus(FriendServiceConstants.FRIEND_STATUS_ACTIVE);

        Friend friend2 = new Friend();
        friend2.setId(snowflake.nextId());
        friend2.setUserId(friendId);
        friend2.setFriendId(userId);
        friend2.setStatus(FriendServiceConstants.FRIEND_STATUS_ACTIVE);

        boolean save1 = this.save(friend1);
        boolean save2 = this.save(friend2);

        if (!save1 || !save2) {
            throw new ServiceException(FriendServiceConstants.ADD_FRIEND_FAILED);
        }
    }

    private Long createSession(Long userId, Long friendId) {
        Long sessionId = snowflake.nextId();
        Session session = new Session();
        session.setId(sessionId);
        session.setName(FriendServiceConstants.EMPTY_STRING);
        session.setType(SessionType.SINGLE.getValue());
        session.setStatus(FriendServiceConstants.FRIEND_STATUS_ACTIVE);

        int sessionSaved = sessionMapper.insert(session);
        if (sessionSaved <= 0) {
            throw new ServiceException(FriendServiceConstants.CREATE_SESSION_FAILED);
        }
        return sessionId;
    }

    private void createUserSessions(Long userId, Long friendId, Long sessionId) {
        UserSession userSession1 = new UserSession();
        userSession1.setId(snowflake.nextId());
        userSession1.setUserId(userId);
        userSession1.setSessionId(sessionId);
        userSession1.setRole(FriendServiceConstants.USER_ROLE_NORMAL);
        userSession1.setStatus(FriendServiceConstants.FRIEND_STATUS_ACTIVE);

        UserSession userSession2 = new UserSession();
        userSession2.setId(snowflake.nextId());
        userSession2.setUserId(friendId);
        userSession2.setSessionId(sessionId);
        userSession2.setRole(FriendServiceConstants.USER_ROLE_NORMAL);
        userSession2.setStatus(FriendServiceConstants.FRIEND_STATUS_ACTIVE);

        int userSessionSaved1 = userSessionMapper.insert(userSession1);
        int userSessionSaved2 = userSessionMapper.insert(userSession2);
        if (userSessionSaved1 <= 0 || userSessionSaved2 <= 0) {
            throw new ServiceException(FriendServiceConstants.CREATE_USER_SESSION_FAILED);
        }
    }

    private void sendPushNotification(User recipient, Long friendId, Long sessionId) throws Exception {
        NewSessionNotification notification = new NewSessionNotification();
        notification.setUserId(String.valueOf(recipient.getUserId()));
        notification.setSessionId(String.valueOf(sessionId));
        notification.setSessionType(SessionType.SINGLE.getValue());
        notification.setSessionName(recipient.getUserName());
        notification.setAvatar(recipient.getAvatar());

        pushService.pushNewSession(friendId, notification);
    }

    private ModifyApplyResponse buildModifyFriendApplicationResponse(User applicant, Long sessionId) {
        ModifyApplyResponse response = new ModifyApplyResponse();
        response.setUserId(String.valueOf(applicant.getUserId()));
        response.setSessionId(String.valueOf(sessionId));
        response.setSessionType(SessionType.SINGLE.getValue());
        response.setSessionName(applicant.getUserName());
        response.setAvatar(applicant.getAvatar());
        return response;
    }

    @Override
    public FriendDetailResponse getFriendDetails(FriendDetailRequest request) {

        User friendUser = userService.getById(request.getFriendUuid());
        validateFriendUser(friendUser);

        FriendDetailResponse response = new FriendDetailResponse();
        response.setUserUuid(String.valueOf(friendUser.getUserId()))
            .setNickname(friendUser.getUserName())
            .setAvatar(friendUser.getAvatar())
            .setEmail(friendUser.getEmail())
            .setPhone(friendUser.getPhone())
            .setSignature(friendUser.getSignature())
            .setGender(friendUser.getGender())
            .setSessionId(populateSessionId(request.getUserUuid(), request.getFriendUuid()))
                .setStatus(populateFriendStatus(request.getUserUuid(), request.getFriendUuid()));

        return response;
    }


    private String populateSessionId(Long userId, Long friendId) {
        List<Long> commonSessionIds = userSessionMapper.findCommonSingleChatSessionIds(userId, friendId);
        if (commonSessionIds == null || commonSessionIds.isEmpty()) {
            return "0";
        } else {
            return String.valueOf(commonSessionIds.get(0));
        }
    }

}