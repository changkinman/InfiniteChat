package com.shanyangcode.infinitechat.messageingservice.controller;

import com.shanyangcode.infinitechat.messageingservice.common.Result;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgRequest;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgResponse;
import com.shanyangcode.infinitechat.messageingservice.feign.ContactServiceFeign;
import com.shanyangcode.infinitechat.messageingservice.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SendMsgController {
    @Autowired
    private ContactServiceFeign contactServiceFeign;

    @Autowired
    private MessageService messageService;


    @GetMapping("/feign")
    public Result<?> getUser() {

        Result<?> user = contactServiceFeign.getUser();

        return Result.OK(user);
    }

    @PostMapping("/v1/chat/session")
    public Result<SendMsgResponse> sendMsg(@RequestBody SendMsgRequest request) throws Exception {
        SendMsgResponse response = messageService.sendMessage(request);

        return Result.OK(response);
    }


}