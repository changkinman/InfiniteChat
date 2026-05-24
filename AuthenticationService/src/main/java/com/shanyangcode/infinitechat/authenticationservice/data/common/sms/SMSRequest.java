package com.shanyangcode.infinitechat.authenticationservice.data.common.sms;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class SMSRequest implements Serializable {
    @NotEmpty(message = "手机号不能为空")
    @Length(min = 11, max = 11, message = "手机号应为 11 位")
    private String phone;
}
