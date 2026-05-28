package com.shanyangcode.infinitechat.contanctservice.controller;

import com.shanyangcode.infinitechat.contanctservice.common.Result;
import com.shanyangcode.infinitechat.contanctservice.data.User.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {
    @GetMapping("/user")
    public Result<UserResponse> getUser() {
        UserResponse userResponse = new UserResponse();
        userResponse.setAvatar("www.baidu.com");

        return Result.OK(userResponse);
    }
}