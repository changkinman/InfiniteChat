package com.shanyangcode.infinitechat.messageingservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 红包主表 Mapper 接口
 */
@Mapper
public interface RedPacketMapper extends BaseMapper<RedPacket> {
    @Select("SELECT * FROM red_packet WHERE red_packet_id = #{redPacketId} FOR UPDATE")
    RedPacket selectByIdForUpdate(@Param("redPacketId") Long redPacketId);
}
