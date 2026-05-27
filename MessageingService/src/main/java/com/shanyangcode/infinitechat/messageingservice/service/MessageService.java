package com.shanyangcode.infinitechat.messageingservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgRequest;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgResponse;
import com.shanyangcode.infinitechat.messageingservice.model.Message;
import org.springframework.stereotype.Service;

@Service
public interface MessageService extends IService<Message> {
    SendMsgResponse sendMessage(SendMsgRequest request);
}