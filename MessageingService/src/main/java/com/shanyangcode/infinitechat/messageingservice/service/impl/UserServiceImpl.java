package com.shanyangcode.infinitechat.messageingservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.infinitechat.messageingservice.mapper.UserMapper;
import com.shanyangcode.infinitechat.messageingservice.model.User;
import com.shanyangcode.infinitechat.messageingservice.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author Zzw
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2024-10-17 14:21:28
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

}




