package com.shangyangcode.infinitechat.contactservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangyangcode.infinitechat.contactservice.model.ApplyFriend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ApplyFriendMapper extends BaseMapper<ApplyFriend> {
    /**
     * 批量更新好友申请状态
     *
     * @param newStatus 新的状态
     * @param targetId  目标用户ID
     * @param userId    用户ID
     * @return 受影响的行数
     */
    @Update("UPDATE apply_friend SET status = #{newStatus} WHERE user_id = #{userId} AND target_id = #{targetId}")
    int updateStatusByUserAndTarget(Integer newStatus, Long userId, Long targetId);


    /**
     * 根据目标用户ID和离线时间查询好友申请
     *
     * @param targetId    目标用户ID
     * @param offlineTime 离线时间
     * @return 好友申请列表
     */
    @Select("SELECT * FROM apply_friend " +
            "WHERE target_id = #{targetId} " +
            "AND created_at >= #{offlineTime} ")
//            + "AND status IN (0, 1, 2)") /* 根据业务需求调整状态筛选条件 */
    List<ApplyFriend> findApplyFriendsByTargetIdAndTime(@Param("targetId") Long targetId,
                                                        @Param("offlineTime") LocalDateTime offlineTime);


}