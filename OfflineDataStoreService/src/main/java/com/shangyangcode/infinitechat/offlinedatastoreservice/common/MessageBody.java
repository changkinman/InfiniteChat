package com.shangyangcode.infinitechat.offlinedatastoreservice.common;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.Data;

@Data
@JSONType(orders = {"receiveUserId", "sendUserId",
"sessionId", "sessionType", "type", "body"})
public class MessageBody {
   protected Long receiveUserId;
   protected Long sendUserId;
   protected Long sessionId;
   protected Long messageId;
   protected Integer type;
   protected Integer sessionType;
   protected Object body;
}
