package com.shanyangcode.infinitechat.momentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.infinitechat.momentservice.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Zzw
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2024-10-08 16:08:49
* @Entity generator.domain.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {
}




