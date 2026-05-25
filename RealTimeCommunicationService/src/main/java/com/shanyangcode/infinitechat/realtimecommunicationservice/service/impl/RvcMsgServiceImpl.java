package com.shanyangcode.infinitechat.realtimecommunicationservice.service.impl;

import com.shanyangcode.infinitechat.realtimecommunicationservice.data.ReceiveMessage.ReceiveMessageRequest;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.ReceiveMessage.ReceiveMessageResponse;
import com.shanyangcode.infinitechat.realtimecommunicationservice.service.RcvMsgServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

@Service
@Slf4j
public class RvcMsgServiceImpl implements RcvMsgServer {

    @Autowired
    private NettyMessageService nettyMessageService;
    @Override
    public ReceiveMessageResponse receiveMessage(@Valid ReceiveMessageRequest request) {
        nettyMessageService.sendMessageToUser(request);

        return new ReceiveMessageResponse();
    }
}