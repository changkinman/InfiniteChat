package com.shangyangcode.infinitechat.offlinedatastoreservice.data.offlineMessage;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OfflineMessageResponse {

    private List<OfflineMessage> offlineMessages;
}