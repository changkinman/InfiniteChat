package com.shangyangcode.infinitechat.contactservice.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.shangyangcode.infinitechat.contactservice.model.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SessionMapper extends MPJBaseMapper<Session> {

    /**
     * 根据用户ID列表、会话类型和离线时间查询会话列表
     *
     * @param userId     用户ID
     * @param sessionType 会话类型（1 单聊，2 群聊）
     * @param offlineTime 用户离线时间
     * @return 会话列表
     */
    @Select({
            "<script>",
            "SELECT s.* FROM session s ",
            "JOIN user_session us ON s.id = us.session_id ",
            "WHERE us.user_id = #{userId}",
            "AND s.type = #{sessionType} ",
            "AND s.created_at >= #{offlineTime} ",
            "AND s.status = 1",
            "</script>"
    })
    List<Session> findSessionsByUserIdsAndType(@Param("userId") Long userId,
                                               @Param("sessionType") int sessionType,
                                               @Param("offlineTime") LocalDateTime offlineTime);

    /**
     * 根据会话ID列表查询会话信息
     *
     * @param sessionIds 会话ID列表
     * @return 会话列表
     */
    @Select({
            "<script>",
            "SELECT * FROM session ",
            "WHERE id IN ",
            "<foreach item='id' collection='sessionIds' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach> ",
            "AND status = 1",
            "</script>"
    })
    List<Session> findSessionsByIds(@Param("sessionIds") List<Long> sessionIds);




}