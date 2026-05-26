package com.shanyangcode.infinitechat.messageingservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.infinitechat.messageingservice.model.Message;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Zzw
* @description 针对表【message】的数据库操作Mapper
* @createDate 2024-11-11 14:37:36
* @Entity generator.domain.Message
*/
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

}




