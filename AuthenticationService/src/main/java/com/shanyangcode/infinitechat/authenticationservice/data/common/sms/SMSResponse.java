package com.shanyangcode.infinitechat.authenticationservice.data.common.sms;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SMSResponse {
    private String phone;
}
