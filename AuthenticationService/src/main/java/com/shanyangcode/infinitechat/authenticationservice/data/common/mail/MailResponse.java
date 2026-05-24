package com.shanyangcode.infinitechat.authenticationservice.data.common.mail;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MailResponse {
    private String mail;
}
