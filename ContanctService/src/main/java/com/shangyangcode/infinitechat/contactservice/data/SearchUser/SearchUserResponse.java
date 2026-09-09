package com.shangyangcode.infinitechat.contactservice.data.SearchUser;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SearchUserResponse {
    private String userUuid;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String signature;

    private Integer gender;

    private Integer status;

    private String sessionId;
}