package com.shangyangcode.infinitechat.offlinedatastoreservice.consumer;

import com.shangyangcode.infinitechat.offlinedatastoreservice.constants.kafka.KafkaConstants;
import com.shangyangcode.infinitechat.offlinedatastoreservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumer {
    @Autowired
    private MessageService messageService;

    @KafkaListener(topics = KafkaConstants.topic, groupId = KafkaConstants.consumerGroupId)
    public void listen(String message){
        messageService.saveOfflineMessage(message);
    }
}