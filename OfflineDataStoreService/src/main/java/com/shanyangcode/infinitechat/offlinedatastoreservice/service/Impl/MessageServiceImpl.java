package com.shanyangcode.infinitechat.offlinedatastoreservice.service.Impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.shangyangcode.infinitechat.offlinedatastoreservice.common.TextMessage;
import com.shangyangcode.infinitechat.offlinedatastoreservice.constants.config.ConfigEnum;
import com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage.*;
import com.shangyangcode.infinitechat.offlinedatastoreservice.mapper.MessageMapper;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.Message;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.RedPacket;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.Session;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.User;
import com.shangyangcode.infinitechat.offlinedatastoreservice.service.MessageService;
import com.shangyangcode.infinitechat.offlinedatastoreservice.service.SessionService;
import com.shangyangcode.infinitechat.offlinedatastoreservice.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private SessionService sessionService;

    @Override
    public OfflineMessageResponse getOfflineMessage(OfflineMessageRequest request) {
        Set<Long> sessionIds = userSessionService.findSessionIdByUserId(request.getUserId());

        OfflineMessageResponse offlineMessageResponse = new OfflineMessageResponse();
        List<OfflineMessage> offlineMessage = new ArrayList<>();

        // 没有聊天，直接返回
        if (sessionIds.isEmpty()){
            return offlineMessageResponse;
        }

        // 否则去获取session中的消息
        HashMap<Long, List<OfflineMessageDetail>> offlineMsgDetails = this.
                findOfflineMsgBySessionId(sessionIds, request.getTime());
        List<Session> sessions = sessionService.listByIds(sessionIds);

        for (Session session : sessions) {
            OfflineMessage offlineMsg = new OfflineMessage();
            offlineMsg.setSessionId(session.getId().toString());
            offlineMsg.setSessionType(session.getType());

            if (session.getType() == Integer.valueOf(ConfigEnum.GROUP_TYPE.getValue())) {
                offlineMsg.setSessionAvatar(ConfigEnum.GROUP_AVATAR.getValue());
                offlineMsg.setSessionName(session.getName());
            }

            List<OfflineMessageDetail> offlineMessageList = offlineMsgDetails.get(session.getId());
            if (offlineMessageList!=null){
                offlineMsg.setOfflineMessageDetails(offlineMessageList);
                offlineMsg.setTotal((long) offlineMessageList.size());

                offlineMessage.add(offlineMsg);
            }

        }

        offlineMessageResponse.setOfflineMessages(offlineMessage);
        return offlineMessageResponse;

    }

    public HashMap<Long, List<OfflineMessageDetail>> findOfflineMsgBySessionId(Set<Long> sessionId, String time) {
        HashMap<Long, List<OfflineMessageDetail>> messageMap = new HashMap<>();

        LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        MPJLambdaWrapper<Message> wrapper = new MPJLambdaWrapper<Message>()
                .selectAll(Message.class)
                .selectAll(RedPacket.class)
                .selectAll(User.class)
                .selectAssociation(RedPacket.class, Message::getRedPacket)
                .selectAssociation(User.class, Message::getUser)
                .in("t.session_id", sessionId)
                .ge("t.created_at", dateTime)
                .leftJoin(RedPacket.class, RedPacket::getRedPacketId, Message::getContent)
                .leftJoin(User.class, User::getUserId, Message::getSenderId);

        List<Message> messages = messageMapper.selectJoinList(Message.class, wrapper);

        log.info("messages:{}", messages);
        for (Message message : messages) {
            OfflineMessageDetail offlineMsgDetail = new OfflineMessageDetail();
            OfflineMessageBody offlineMessageBody = new OfflineMessageBody();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            offlineMessageBody.setCreatedAt(formatter.format(message.getCreatedAt()));
            offlineMessageBody.setContent(message.getContent());

            if (message.getReplyId() != null) {
                offlineMessageBody.setReplyId(message.getReplyId().toString());
            }

            offlineMsgDetail.setOfflineMessageBody(offlineMessageBody);

            //如果是红包则设置红包封面文案
            if (message.getType().equals(Integer.valueOf(ConfigEnum.MESSAGE_TYPE.getValue()))) {
                OfflineRedPacketMessageBody body = new OfflineRedPacketMessageBody(message.getRedPacket().getRedPacketWrapperText());
                BeanUtils.copyProperties(offlineMessageBody, body);

                offlineMsgDetail.setOfflineMessageBody(body);
            }

            // 设置发送人信息
            offlineMsgDetail.setUserName(message.getUser().getUserName());
            offlineMsgDetail.setAvatar(message.getUser().getAvatar());

            // 设置消息详情信息
            offlineMsgDetail.setMessageId(message.getMessageId().toString());
            offlineMsgDetail.setSendUserId(message.getSenderId().toString());
            offlineMsgDetail.setType(message.getType());

            // 如果不存在当前 sessionId 则需要初始化
            if (!messageMap.containsKey(message.getSessionId())){
                messageMap.put(message.getSessionId(), new ArrayList<>());
            }

            messageMap.get(message.getSessionId()).add(offlineMsgDetail);
        }

        return messageMap;
    }

    public void saveOfflineMessage(String message) {
        TextMessage textMessage = JSONUtil.toBean(message, TextMessage.class);
        Message msg = new Message();
        BeanUtil.copyProperties(textMessage, msg);

        msg.setContent(textMessage.getBody().getContent());
        msg.setReplyId(textMessage.getBody().getReplyId());
        msg.setSenderId(textMessage.getSendUserId());

        int insert = this.baseMapper.insert(msg);

        if (insert <= 0) {
            throw new RuntimeException("保存离线消息失败");
        }
    }
}




