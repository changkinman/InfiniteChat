package com.shanyangcode.infinitechat.authenticationservice.controller;

import com.shanyangcode.infinitechat.authenticationservice.common.Result;
import com.shanyangcode.infinitechat.authenticationservice.data.user.login.LoginRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.login.LoginResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode.LoginCodeRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode.LoginCodeResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.register.RegisterRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.register.RegisterResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.updateAvatar.UpdateAvatarRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.updateAvatar.UpdateAvatarResponse;
import com.shanyangcode.infinitechat.authenticationservice.service.UserService;
import com.shanyangcode.infinitechat.authenticationservice.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@Slf4j
@RequestMapping("/api/v1/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) throws InterruptedException {
        RegisterResponse response = userService.register(request);

        return Result.OK(response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> register(@Valid @RequestBody LoginRequest request){
        LoginResponse response = userService.login(request);

        return Result.OK(response);
    }

    @PostMapping("/loginCode")
    public Result<LoginCodeResponse> register(@Valid @RequestBody LoginCodeRequest request){
        LoginCodeResponse response = userService.loginCode(request);

        return Result.OK(response);
    }

    // 获取用户信息接口
    // @GetMapping("/info")

    @PatchMapping("/avatar")
    public Result<UpdateAvatarResponse> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request,
                                                     @RequestHeader String Authorization) throws Exception {
        String id = JwtUtil.parse(Authorization).getSubject();
        UpdateAvatarResponse response = userService.updateAvatar(id, request);

        return Result.OK(response);
    }
}

