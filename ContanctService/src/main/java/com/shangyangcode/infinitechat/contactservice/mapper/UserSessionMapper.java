package com.shangyangcode.infinitechat.contactservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangyangcode.infinitechat.contactservice.model.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {
    /**
     * 根据用户ID、会话类型和离线时间查询群聊会话关系
     *
     * @param userId      用户ID
     * @param sessionType 会话类型（2 群聊）
     * @param offlineTime 用户离线时间
     * @return 用户会话关系列表
     */
    @Select("SELECT * FROM user_session " +
            "WHERE user_id = #{userId} " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE type = #{sessionType} " +
            "AND created_at >= #{offlineTime} " +
            "AND status = 1" +
            ") " +
            "AND status = 1")
    List<UserSession> findGroupSessionsByUserIdAndTime(@Param("userId") Long userId,
                                                       @Param("sessionType") int sessionType,
                                                       @Param("offlineTime") LocalDateTime offlineTime);

    /**
     * 查询两个用户之间共有的单聊 sessionId
     *
     * @param userId1 第一个用户的ID
     * @param userId2 第二个用户的ID
     * @return 共有的单聊 sessionId 列表
     */
    @Select("SELECT us1.session_id " +
            "FROM user_session us1 " +
            "INNER JOIN user_session us2 ON us1.session_id = us2.session_id " +
            "INNER JOIN session s ON us1.session_id = s.id " +
            "WHERE us1.user_id = #{userId1} " +
            "AND us2.user_id = #{userId2} " +
            "AND s.type = 1 " +
            "AND s.status = 1 " +
            "AND us1.status = 1 " +
            "AND us2.status = 1")
    List<Long> findCommonSingleChatSessionIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

}
