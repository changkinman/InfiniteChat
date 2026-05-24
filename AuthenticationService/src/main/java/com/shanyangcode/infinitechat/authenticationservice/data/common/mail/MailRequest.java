package com.shanyangcode.infinitechat.authenticationservice.data.common.mail;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class MailRequest {
    @NotEmpty(message = "邮箱不能为空")
    private String mail;
}
