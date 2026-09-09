package com.shanyangcode.infinitechat.momentservice.service.impl;


import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.infinitechat.momentservice.Exception.DatabaseException;
import com.shanyangcode.infinitechat.momentservice.Exception.UserException;
import com.shanyangcode.infinitechat.momentservice.common.Result;
import com.shanyangcode.infinitechat.momentservice.constants.ConfigEnum;
import com.shanyangcode.infinitechat.momentservice.constants.ErrorEnum;
import com.shanyangcode.infinitechat.momentservice.constants.MomentConstants;
import com.shanyangcode.infinitechat.momentservice.data.createLike.CreateLikeRequest;
import com.shanyangcode.infinitechat.momentservice.data.createLike.CreateLikeResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteLike.DeleteLikeRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteLike.DeleteLikeResponse;
import com.shanyangcode.infinitechat.momentservice.mapper.MomentLikeMapper;
import com.shanyangcode.infinitechat.momentservice.model.MomentLike;
import com.shanyangcode.infinitechat.momentservice.service.MomentLikeService;
import com.shanyangcode.infinitechat.momentservice.service.MomentNotificationService;
import com.shanyangcode.infinitechat.momentservice.service.MomentService;
import com.shanyangcode.infinitechat.momentservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 朋友圈点赞服务实现类
 * 处理朋友圈点赞的创建和删除
 */
@Service
@Slf4j
public class MomentLikeServiceImpl extends ServiceImpl<MomentLikeMapper, MomentLike> implements MomentLikeService {
    @Autowired
    private MomentNotificationService notificationService;

    @Autowired
    @Lazy  // 使用延迟加载避免循环依赖
    private MomentService momentService;

    /**
     * 创建点赞并发送通知
     *
     * @param momentId 朋友圈ID
     * @param userId 用户ID
     * @return 点赞ID
     * @throws Exception 可能发生的异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createLikeWithNotification(Long momentId, Long userId) throws Exception {
        // 创建点赞
        Long likeId = createLike(momentId, userId);

        // 获取朋友圈所有者ID
        Long momentOwnerId = momentService.getMomentOwnerId(momentId);
        List<Long> receiverIds = new ArrayList<>();

        // 如果是自己点赞自己的朋友圈，不发送通知
        if (momentOwnerId != null && !momentOwnerId.equals(userId)) {
            receiverIds.add(momentOwnerId);
            // 发送点赞通知
            notificationService.sendInteractionNotification(userId, momentId, receiverIds);
        }

        return likeId;
    }

    /**
     * 创建点赞
     *
     * @param momentId 朋友圈ID
     * @param userId   点赞用户ID
     * @return 创建的点赞ID
     */
    @Override
    public Long createLike(Long momentId, Long userId) {
        // 创建点赞实体
        MomentLike like = createLikeEntity(momentId, userId);

        // 保存到数据库
        this.save(like);

        log.debug("用户 {} 对朋友圈 {} 创建了点赞, 点赞ID: {}", userId, momentId, like.getLikeId());

        return like.getLikeId();
    }

    @Override
    public CreateLikeResponse likeMomentResponse(Long momentId, CreateLikeRequest request) throws Exception {
        Long likeId = createLikeWithNotification(momentId, request.getUserId());

        CreateLikeResponse response = new CreateLikeResponse();
        response.setLikeId(likeId);

        return response;
    }

    @Override
    public DeleteLikeResponse deleteLikeMoment(DeleteLikeRequest request) {
        QueryWrapper<MomentLike> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(MomentConstants.FIELD_MOMENT_ID, request.getMomentId())
                .eq(MomentConstants.FIELD_LIKE_ID, request.getLikeId())
                .eq(MomentConstants.FIELD_USER_ID, request.getUserId());

        MomentLike like = this.getOne(queryWrapper);
        if (like == null){
            log.error("删除点赞失败：找不到点赞记录，朋友圈ID: {}, 点赞ID: {}, 用户ID: {}", request.getMomentId(), request.getLikeId(), request.getUserId());

            throw new UserException(ErrorEnum.DELETE_Like_FAILED_MSG);
        }

        like.setIsDelete(MomentConstants.DELETED);
        like.setUpdateTime(new Date());

        boolean update = this.update(like, queryWrapper);
        if (!update){
            throw new DatabaseException(ErrorEnum.DATABASE_ERROR.getCode(), ErrorEnum.DATABASE_ERROR.getMessage());
        }

        return new DeleteLikeResponse().setMessage(MomentConstants.DELETE_LIKE_SUCCESS_MSG);
    }

    /**
     * 创建点赞实体
     *
     * @param momentId 朋友圈ID
     * @param userId   用户ID
     * @return 点赞实体
     */
    private MomentLike createLikeEntity(Long momentId, Long userId) {
        MomentLike like = new MomentLike();

        // 使用雪花算法生成ID
        Snowflake snowflake = IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
        );

        like.setLikeId(snowflake.nextId());
        like.setMomentId(momentId);
        like.setUserId(userId);
        like.setIsDelete(MomentConstants.NOT_DELETED);

        return like;
    }

}
