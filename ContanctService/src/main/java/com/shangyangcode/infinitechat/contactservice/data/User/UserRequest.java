package com.shangyangcode.infinitechat.contactservice.data.User;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserRequest {
    private String phone;
}