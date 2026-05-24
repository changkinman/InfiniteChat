package com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class LoginCodeRequest {
    /**
     * 手机号
     */
    @NotEmpty(message = "手机号不能为空")
    @Length(min = 11, max = 11, message = "手机号应为 11 位")
    private String phone;

    /**
     * 验证码
     */
    @NotEmpty(message = "验证码不能为空")
    @Length(min = 6, max = 6, message = "验证码应为 6 位")
    private String code;
}
