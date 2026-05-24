package com.shanyangcode.infinitechat.authenticationservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shanyangcode.infinitechat.authenticationservice.data.user.login.LoginRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.login.LoginResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode.LoginCodeRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode.LoginCodeResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.register.RegisterRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.register.RegisterResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.updateAvatar.UpdateAvatarRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.updateAvatar.UpdateAvatarResponse;
import com.shanyangcode.infinitechat.authenticationservice.model.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-05-23 14:11:49
*/
public interface UserService extends IService<User> {
    default User getOnly(QueryWrapper<User> wrapper, boolean throwEx){
        wrapper.last("limit 1");

        return this.getOne(wrapper, throwEx);
    }

    RegisterResponse register(RegisterRequest request) throws InterruptedException;

    LoginResponse login(LoginRequest request);

    LoginCodeResponse loginCode(LoginCodeRequest request);

    UpdateAvatarResponse updateAvatar(String id, UpdateAvatarRequest request);
}
