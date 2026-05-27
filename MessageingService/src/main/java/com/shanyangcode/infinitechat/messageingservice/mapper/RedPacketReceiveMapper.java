package com.shanyangcode.infinitechat.messageingservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacketReceive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 红包领取记录表 Mapper 接口
 */
@Mapper
public interface RedPacketReceiveMapper extends BaseMapper<RedPacketReceive> {
    /**
     * 根据红包ID查询领取记录
     *
     * @param redPacketId 红包ID
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 红包领取记录列表
     */
    @Select("SELECT * FROM red_packet_receive WHERE red_packet_id = #{redPacketId} " +
            "LIMIT #{pageNum}, #{pageSize}")
    List<RedPacketReceive> selectByRedPacketId(@Param("redPacketId") Long redPacketId,
                                               @Param("pageNum") Integer pageNum,
                                               @Param("pageSize") Integer pageSize);
}