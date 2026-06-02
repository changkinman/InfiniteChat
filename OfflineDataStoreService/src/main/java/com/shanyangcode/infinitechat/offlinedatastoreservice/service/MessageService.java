package com.shanyangcode.infinitechat.offlinedatastoreservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage.OfflineMessageRequest;
import com.shanyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage.OfflineMessageResponse;
import com.shanyangcode.infinitechat.offlinedatastoreservice.model.Message;


public interface MessageService extends IService<Message> {

    OfflineMessageResponse getOfflineMessage(OfflineMessageRequest request);

    void saveOfflineMessage(String message);
}
