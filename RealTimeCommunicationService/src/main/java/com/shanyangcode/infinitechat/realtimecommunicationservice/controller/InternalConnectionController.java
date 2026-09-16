package com.shanyangcode.infinitechat.realtimecommunicationservice.controller;

import com.shanyangcode.infinitechat.realtimecommunicationservice.common.Result;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.connection.ConnectionTakeoverRequest;
import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ChannelManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/internal/connections")
public class InternalConnectionController {

    private final ChannelManager channelManager;

    public InternalConnectionController(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    @PostMapping("/takeover")
    public Result<Void> takeover(@Valid @RequestBody ConnectionTakeoverRequest request) {
        channelManager.closeIfMatches(request.getUserId(), request.getConnectionId());
        return Result.OK(null);
    }
}
