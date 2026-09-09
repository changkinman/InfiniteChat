package com.shanyangcode.infinitechat.realtimecommunicationservice.controller;

import com.shanyangcode.infinitechat.realtimecommunicationservice.common.Result;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.PushMoment.FriendApplicationNotification;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.PushMoment.NewGroupSessionNotification;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.PushMoment.NewSessionNotification;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.PushMoment.PushMomentRequest;
import com.shanyangcode.infinitechat.realtimecommunicationservice.service.impl.NettyMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1/message/push")
public class PushController {

    @Autowired
    private NettyMessageService nettyMessageService;

    @PostMapping("/moment")
    public Result<?> receiveNoticeMoment(@RequestBody PushMomentRequest request){
        nettyMessageService.sendNoticeMoment(request);
        return Result.OK(null);
    }

    @PostMapping("/friendApplication/{userId}")
    public Result<?> pushFriendApplication(
            @PathVariable("userId") String userId,
            @RequestBody FriendApplicationNotification notification
    ) {
        nettyMessageService.sendFriendApplicationNotification(notification, userId);

        return Result.OK("Friend application notification pushed.");
    }

    @PostMapping("/newSession/{userId}")
    public Result pushNewSession(
            @PathVariable("userId") String userId,
            @RequestBody NewSessionNotification notification
    ) {
        nettyMessageService.sendNewSessionNotification(notification, userId);
        return Result.OK("New session notification pushed.");
    }

    @PostMapping("/newGroupSession/{userId}")
    public Result pushNewGroupSession(
            @PathVariable("userId") String userId,
            @RequestBody NewGroupSessionNotification notification
    ) {
        nettyMessageService.sendNewGroupSessionNotification(notification, userId);
        return Result.OK("New Group session notification pushed.");
    }
}