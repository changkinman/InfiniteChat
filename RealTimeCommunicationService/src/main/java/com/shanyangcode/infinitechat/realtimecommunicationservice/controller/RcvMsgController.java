package com.shanyangcode.infinitechat.realtimecommunicationservice.controller;

import com.shanyangcode.infinitechat.realtimecommunicationservice.common.Result;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.ReceiveMessage.ReceiveMessageRequest;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.ReceiveMessage.ReceiveMessageResponse;
import com.shanyangcode.infinitechat.realtimecommunicationservice.service.RcvMsgServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/message")
@Slf4j
@RequiredArgsConstructor
public class RcvMsgController {
    @Autowired
    private RcvMsgServer  rcvMsgServer;

    @PostMapping("/user")
    public Result<ReceiveMessageResponse> receiveMessage(@RequestBody ReceiveMessageRequest request){
        ReceiveMessageResponse response = rcvMsgServer.receiveMessage(request);

        return Result.OK(response);
    }
}