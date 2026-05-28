package com.shanyangcode.infinitechat.contanctservice.data.User;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserRequest {
    private String phone;
}