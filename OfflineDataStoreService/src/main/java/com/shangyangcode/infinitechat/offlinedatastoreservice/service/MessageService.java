package com.shangyangcode.infinitechat.offlinedatastoreservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage.OfflineMessageRequest;
import com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage.OfflineMessageResponse;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.Message;


public interface MessageService extends IService<Message> {

    OfflineMessageResponse getOfflineMessage(OfflineMessageRequest request);

    void saveOfflineMessage(String message);
}
