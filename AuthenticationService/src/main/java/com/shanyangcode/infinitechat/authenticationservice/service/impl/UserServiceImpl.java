package com.shanyangcode.infinitechat.authenticationservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.infinitechat.authenticationservice.constants.config.ConfigEnum;
import com.shanyangcode.infinitechat.authenticationservice.constants.user.ErrorEnum;
import com.shanyangcode.infinitechat.authenticationservice.data.user.login.LoginRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.login.LoginResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode.LoginCodeRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.loginCode.LoginCodeResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.register.RegisterRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.register.RegisterResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.user.updateAvatar.UpdateAvatarRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.user.updateAvatar.UpdateAvatarResponse;
import com.shanyangcode.infinitechat.authenticationservice.exception.CodeException;
import com.shanyangcode.infinitechat.authenticationservice.exception.DatabaseException;
import com.shanyangcode.infinitechat.authenticationservice.exception.UserException;
import com.shanyangcode.infinitechat.authenticationservice.mapper.UserBalanceMapper;
import com.shanyangcode.infinitechat.authenticationservice.model.User;
import com.shanyangcode.infinitechat.authenticationservice.mapper.UserMapper;
import com.shanyangcode.infinitechat.authenticationservice.model.UserBalance;
import com.shanyangcode.infinitechat.authenticationservice.service.UserService;
import com.shanyangcode.infinitechat.authenticationservice.utils.JwtUtil;
import com.shanyangcode.infinitechat.authenticationservice.utils.NickNameGeneratorUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import cn.hutool.core.util.IdUtil;

import com.shanyangcode.infinitechat.authenticationservice.constants.user.registerConstant;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserBalanceMapper userBalanceMapper;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        String phone = request.getPhone();
        String password = request.getPassword();

        if (isRegister(phone)){
            throw new UserException(ErrorEnum.REGISTER_ERROR);
        }

        // 去查redis code == redisCode
        String redisCode = redisTemplate.opsForValue().get(registerConstant.REGISTER_CODE + phone);
        if (redisCode == null || !redisCode.equals(request.getCode())){
            throw new CodeException(ErrorEnum.CODE_ERROR);
        }
        // 相等就 存数据库
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());

        User user = new User()
                .setUserId(snowflake.nextId())
                .setPassword(encryptedPassword)
                .setPhone(phone)
                .setUserName(NickNameGeneratorUtil.generateNickName());

        boolean isUserSave = this.save(user);
        if (!isUserSave){
            throw new DatabaseException("数据库异常，保存用户信息失败");
        }

        UserBalance userBalance = new UserBalance()
                .setUserId(user.getUserId())
                .setBalance(BigDecimal.valueOf(1000))
                .setUpdatedAt(LocalDateTime.now());

        int insert = userBalanceMapper.insert(userBalance);
        if (insert <= 0){
            throw new DatabaseException("数据库异常，创建用户账户信息错误");
        }

        return new RegisterResponse().setPhone(phone);
    }

    private boolean isRegister(String phone){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);

        long count = this.count(queryWrapper);

        return count > 0;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", request.getPhone());

        User user = this.getOnly(queryWrapper, true);
        String password = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (user == null || !password.equals(user.getPassword())){
            throw new UserException(ErrorEnum.LOGIN_ERROR);
        }

        LoginResponse response = new LoginResponse();
        BeanUtils.copyProperties(user, response);
        response.setUserId(user.getUserId().toString());
        String token = JwtUtil.generate(String.valueOf(user.getUserId()));
        response.setToken(token);
        return response;
    }


    @Override
    public LoginCodeResponse loginCode(LoginCodeRequest request) {
        String redisCode = redisTemplate.opsForValue().get(registerConstant.REGISTER_CODE + request.getPhone());
        if (redisCode == null || !redisCode.equals(request.getCode())) {
            throw new CodeException(ErrorEnum.CODE_ERROR);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", request.getPhone());
        User user = this.getOnly(queryWrapper, true);
        if (user == null) {
            throw new UserException(ErrorEnum.LOGIN_ERROR);
        }

        LoginCodeResponse response = new LoginCodeResponse();
        BeanUtil.copyProperties(user, response);

        String token = JwtUtil.generate(response.getUserId());
        response.setToken(token);

        return response;
    }

    @Override
    public UpdateAvatarResponse updateAvatar(String id, UpdateAvatarRequest request) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", Long.valueOf(id));
        User user = this.getOnly(queryWrapper, true);
        if (user == null) {
            throw new UserException(ErrorEnum.NO_USER_ERROR);
        }

        user.setAvatar(request.avatarUrl);
        boolean isUpdate = this.updateById(user);
        if (!isUpdate) {
            throw new DatabaseException(ErrorEnum.UPDATE_AVATAR_ERROR);
        }

        UpdateAvatarResponse response = new UpdateAvatarResponse();
        BeanUtil.copyProperties(user, response);

        return response;
    }
}




