package com.shangyangcode.infinitechat.contactservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangyangcode.infinitechat.contactservice.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}