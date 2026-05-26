package com.shanyangcode.infinitechat.messageingservice.service;

import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgRequest;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgResponse;
import org.springframework.stereotype.Service;

@Service
public interface MessageService {
    SendMsgResponse sendMessage(SendMsgRequest request);
}