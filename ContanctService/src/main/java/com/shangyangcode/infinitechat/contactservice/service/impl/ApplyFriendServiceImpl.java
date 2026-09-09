package com.shangyangcode.infinitechat.contactservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.contactservice.constants.ConfigEnum;
import com.shangyangcode.infinitechat.contactservice.constants.ErrorEnum;
import com.shangyangcode.infinitechat.contactservice.constants.FriendApplicationStatus;
import com.shangyangcode.infinitechat.contactservice.constants.FriendRequestConstants;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.AddFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.AddFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.FriendApplicationNotification;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.ApplyListRequest;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.ApplyListResponse;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.applyFriend;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyRequest;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyResponse;
import com.shangyangcode.infinitechat.contactservice.data.UnreadApply.UnreadApplyRequest;
import com.shangyangcode.infinitechat.contactservice.data.UnreadApply.UnreadApplyResponse;
import com.shangyangcode.infinitechat.contactservice.exception.DatabaseException;
import com.shangyangcode.infinitechat.contactservice.exception.ServiceException;
import com.shangyangcode.infinitechat.contactservice.mapper.ApplyFriendMapper;
import com.shangyangcode.infinitechat.contactservice.model.ApplyFriend;
import com.shangyangcode.infinitechat.contactservice.model.User;
import com.shangyangcode.infinitechat.contactservice.service.ApplyFriendService;
import com.shangyangcode.infinitechat.contactservice.service.FriendService;
import com.shangyangcode.infinitechat.contactservice.service.PushService;
import com.shangyangcode.infinitechat.contactservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 好友申请服务实现类
 */
@Slf4j
@Service
public class ApplyFriendServiceImpl extends ServiceImpl<ApplyFriendMapper, ApplyFriend> implements ApplyFriendService {
    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PushService pushService;

    @Autowired
    private ApplyFriendMapper applyFriendMapper;

    @Autowired
    private FriendService friendService;

    @Override
    public AddFriendResponse addFriend(String userUuid, String receiveUserUuid, AddFriendRequest request) throws Exception {
        Long senderId = Long.valueOf(userUuid);
        Long receiverId = Long.valueOf(receiveUserUuid);

        User senderUser = getUserById(senderId);
        User receiverUser = getUserById(receiverId);

        FriendApplicationNotification notification = createNotification(senderUser);
        ApplyFriend existingApplyFriend = findExistingApplyFriend(senderId, receiverId);

        if (existingApplyFriend == null){
            handleNewFriendApplication(senderId, receiverId, request.getMsg(), notification);
        }

        return new AddFriendResponse();
    }

    private User getUserById(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(userId + "用户不存在");
        }
        return user;
    }

    private FriendApplicationNotification createNotification(User applicant) {
        FriendApplicationNotification notification = new FriendApplicationNotification();

        notification.setApplyUserName(applicant.getUserName());
        return notification;
    }

    private ApplyFriend findExistingApplyFriend(Long senderId, Long receiverId) {
        QueryWrapper<ApplyFriend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(FriendRequestConstants.USER_ID, senderId)
                .eq(FriendRequestConstants.TARGET_ID, receiverId);

        return this.getOne(queryWrapper);
    }

    private void handleNewFriendApplication(Long senderId, Long receiverId, String msg, FriendApplicationNotification notification) {
        ApplyFriend newApplyFriend = createApplyFriend(senderId, receiverId, msg);
        boolean isSaved = this.save(newApplyFriend);
        if (isSaved) {
            setFriendRequestExpiration(newApplyFriend.getId());
        }else {
            throw new DatabaseException(ErrorEnum.DATABASE_ERROR.getCode(), ErrorEnum.DATABASE_ERROR.getMessage());
        }

        pushNotification(receiverId, notification);
    }

    private ApplyFriend createApplyFriend(Long senderId, Long receiverId, String msg) {
        Snowflake snowflake = IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue()));

        ApplyFriend applyFriend = new ApplyFriend();
        applyFriend.setId(snowflake.nextId());
        applyFriend.setUserId(senderId);
        applyFriend.setTargetId(receiverId);
        applyFriend.setMsg(msg);
        applyFriend.setStatus(FriendApplicationStatus.UNREAD.getCode());
        return applyFriend;
    }

    private void setFriendRequestExpiration(Long applyFriendId) {
        String redisKey = FriendRequestConstants.FRIEND_REQUEST_KEY_PREFIX + applyFriendId;
        redisTemplate.opsForValue().set(redisKey, FriendRequestConstants.ACTIVE_STATUS,
                FriendRequestConstants.FRIEND_REQUEST_EXPIRATION_SECONDS, TimeUnit.SECONDS);
    }

    private void pushNotification(Long receiverId, FriendApplicationNotification notification) {
        try {
            pushService.pushNewApply(receiverId, notification);
        } catch (Exception e) {
            log.warn(FriendRequestConstants.PUSH_FAILURE_LOG, e.getMessage());
        }
    }

    @Override
    public ApplyListResponse getApplyList(ApplyListRequest request) {
        ApplyListResponse applyListResponse = new ApplyListResponse();

        Page<ApplyFriend> page = new Page<>(request.getPageNum(), request.getPageSize());
        QueryWrapper<ApplyFriend> queryWrapper = buildApplyListQuery(request.getUserUuid(), request.getKey());
        Page<ApplyFriend> applyFriendPage = applyFriendMapper.selectPage(page, queryWrapper);

        long total = applyFriendPage.getTotal();
        List<ApplyFriend> applyFriendList = applyFriendPage.getRecords();

        if (total == 0){
            return applyListResponse;
        }

        HashSet<Long> userUuidSet = new HashSet<>();
        for (ApplyFriend applyFriend : applyFriendList) {
            userUuidSet.add(applyFriend.getUserId());
            userUuidSet.add(applyFriend.getTargetId());
        }

        HashMap<Long, User> userMap = getUserInfo(userUuidSet);

        ArrayList<applyFriend> applyFriends = new ArrayList<>();

        for (ApplyFriend applyFriend : applyFriendList) {
            applyFriend apply = new applyFriend();
            apply.setMsg(applyFriend.getMsg());
            apply.setStatus(applyFriend.getStatus());
            apply.setTime(applyFriend.getCreatedAt());

            if (applyFriend.getUserId().equals(request.getUserUuid())) {
                if (userMap.containsKey(applyFriend.getTargetId())){
                    User targetUser = userMap.get(applyFriend.getUserId());

                    apply.setUserUuid(String.valueOf(targetUser.getUserId()));
                    apply.setNickname(targetUser.getUserName());
                    apply.setAvatar(targetUser.getAvatar());
                    apply.setIsReceiver(FriendRequestConstants.IS_RECEIVER_NO);
                }

            } else {
                if (userMap.containsKey(applyFriend.getUserId())){
                    User senderUser = userMap.get(applyFriend.getUserId());

                    apply.setUserUuid(String.valueOf(senderUser.getUserId()));
                    apply.setNickname(senderUser.getUserName());
                    apply.setAvatar(senderUser.getAvatar());
                    apply.setIsReceiver(FriendRequestConstants.IS_RECEIVER_YES);
                }
            }

            applyFriends.add(apply);
        }

        applyListResponse.setData(applyFriends);
        applyListResponse.setTotal(total);

        return applyListResponse;
    }

    private QueryWrapper<ApplyFriend> buildApplyListQuery(Long userUuid, String key) {
        QueryWrapper<ApplyFriend> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq("user_id", userUuid).or().eq("target_id", userUuid));

        if (key != null && !key.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like("msg", key));
        }

        return queryWrapper;
    }


    private HashMap<Long, User> getUserInfo(HashSet<Long> userUuidSet){
        QueryWrapper<User> userIdQueryWrapper = new QueryWrapper<User>().in("user_id", userUuidSet);
        List<User> userList = userService.list(userIdQueryWrapper);

        HashMap<Long, User> userMap = new HashMap<>();
        for(User user: userList){
            userMap.put(user.getUserId(), user);
        }

        return userMap;
    }

    @Override
    public UnreadApplyResponse getUnreadApply(UnreadApplyRequest request) {
        QueryWrapper<ApplyFriend> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("target_id", request.getUserUuid())
                .eq("status", FriendApplicationStatus.UNREAD.getCode());


        long count = this.count(queryWrapper);

        UnreadApplyResponse response = new UnreadApplyResponse();
        response.setCount(count);

        return response;
    }

    @Override
    @Transactional
    public ModifyApplyResponse modifyApply(ModifyApplyRequest request) throws Exception {
        FriendApplicationStatus newStatus = FriendApplicationStatus.fromCode(request.getStatus());

        switch (newStatus) {
            case ACCEPTED:
                return handleAcceptStatus(request.getUserUuid(), request.getReceiveUserUuids());
            case READ:
                return handleReadStatus(request.getUserUuid(), request.getReceiveUserUuids());
            case REJECTED:
            case EXPIRED:
            default:
                throw new ServiceException("不允许修改为该状态值");
        }
    }

    private ModifyApplyResponse handleAcceptStatus(Long userId, List<Long> receiveUserIds) throws Exception {
        Long receiveUserId = receiveUserIds.get(0);

        QueryWrapper<ApplyFriend> applyFriendQueryWrapper = new QueryWrapper<>();
        applyFriendQueryWrapper.eq("user_id", userId).in("target_id", receiveUserId);
        applyFriendMapper.update(new ApplyFriend().setStatus(FriendApplicationStatus.ACCEPTED.getCode()), applyFriendQueryWrapper);

        return friendService.addFriend(userId, receiveUserId);
    }

    private ModifyApplyResponse handleReadStatus(Long userId, List<Long> receiveUserIds) {
        UpdateWrapper<ApplyFriend> updateWrapper = new UpdateWrapper<ApplyFriend>()
                .set(FriendRequestConstants.STATUS, FriendApplicationStatus.READ.getCode())
                .eq(FriendRequestConstants.TARGET_ID, userId)
                .in(FriendRequestConstants.USER_ID, receiveUserIds)
                .eq(FriendRequestConstants.STATUS, FriendApplicationStatus.UNREAD.getCode());

        applyFriendMapper.update(null,updateWrapper);

        return null;
    }
}