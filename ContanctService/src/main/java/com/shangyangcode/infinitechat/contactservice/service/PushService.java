package com.shangyangcode.infinitechat.contactservice.service;

import com.shangyangcode.infinitechat.contactservice.data.AddFriend.FriendApplicationNotification;
import com.shangyangcode.infinitechat.contactservice.data.dto.push.NewGroupSessionNotification;
import com.shangyangcode.infinitechat.contactservice.data.dto.push.NewSessionNotification;

public interface PushService {

    /**
     * 推送好友申请
     *
     * @param userId
     * @param notification
     * @throws Exception
     */
    void pushNewApply(Long userId, FriendApplicationNotification notification) throws Exception;

    void pushGroupNewSession(Long userId, NewGroupSessionNotification notification) throws Exception;

    void pushNewSession(Long userId, NewSessionNotification notification) throws Exception;

}