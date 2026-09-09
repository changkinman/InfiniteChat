package com.shangyangcode.infinitechat.contactservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.contactservice.mapper.UserMapper;
import com.shangyangcode.infinitechat.contactservice.model.User;
import com.shangyangcode.infinitechat.contactservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}