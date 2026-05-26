package com.shanyangcode.infinitechat.messageingservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.infinitechat.messageingservice.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 奇奇怪怪的沙小石
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2024-10-17 14:21:28
* @Entity generator.domain.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




