package com.shanyangcode.infinitechat.momentservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.shanyangcode.infinitechat.momentservice.Exception.DatabaseException;
import com.shanyangcode.infinitechat.momentservice.Exception.UserException;
import com.shanyangcode.infinitechat.momentservice.constants.ConfigEnum;
import com.shanyangcode.infinitechat.momentservice.constants.ErrorEnum;
import com.shanyangcode.infinitechat.momentservice.constants.MomentConstants;
import com.shanyangcode.infinitechat.momentservice.data.createMoment.CreateMomentRequest;
import com.shanyangcode.infinitechat.momentservice.data.createMoment.CreateMomentResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteMoment.DeleteMomentRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteMoment.DeleteMomentResponse;
import com.shanyangcode.infinitechat.momentservice.mapper.MomentMapper;
import com.shanyangcode.infinitechat.momentservice.model.Moment;
import com.shanyangcode.infinitechat.momentservice.model.MomentComment;
import com.shanyangcode.infinitechat.momentservice.model.MomentLike;
import com.shanyangcode.infinitechat.momentservice.model.User;
import com.shanyangcode.infinitechat.momentservice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class MomentServiceImpl extends ServiceImpl<MomentMapper, Moment> implements MomentService {
    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    @Autowired
    private MomentLikeService momentLikeService;

    @Autowired
    private MomentCommentService momentCommentService;

    @Autowired
    private MomentNotificationService notificationService;

    private final Gson gson = new Gson();

    @Override
    public CreateMomentResponse createMoment(CreateMomentRequest request) throws Exception {
        Long userId = Long.valueOf(request.getUserId());

        return crateMomentWithNotification(userId, request.getText(), request.getMediaUrls());
    }

    private CreateMomentResponse crateMomentWithNotification(Long userId, String text, List<String> mediaUrls) throws Exception {
        // 保存朋友圈
        CreateMomentResponse response = saveMoment(userId, text, mediaUrls);
        // 获取用户头像
        User user = userService.getById(userId);
        String avatar = user != null ? user.getAvatar() : null;

        // 发通知，发送给我的朋友
        List<Long> friendIds = friendService.getFriendIds(userId);

        // 发送朋友圈创建通知
        notificationService.sendMomentCreationNotification(userId, avatar, response.getMomentId(), friendIds);

        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateMomentResponse saveMoment(Long userId, String text, List<String> urls) {
        // 将URL列表转换为JSON字符串
        String mediaUrls = gson.toJson(urls);

        // 创建朋友圈实体
        Moment moment = createMomentEntity(userId, text, mediaUrls);

        // 保存到数据库
        if (!this.save(moment)) {
            throw new DatabaseException(ErrorEnum.DATABASE_ERROR.getCode(),MomentConstants.ERROR_SAVE_FAILED);
        }

        // 转换为VO对象返回
        return convertToMomentVO(moment, urls);
    }

    private Moment createMomentEntity(Long userId, String text, String mediaUrls) {
        Moment moment = new Moment();

        Snowflake snowflake = createSnowflake();
        moment.setUserId(userId);
        moment.setText(text);
        moment.setMediaUrl(mediaUrls);
        moment.setMomentId(snowflake.nextId());

        return moment;
    }

    private Snowflake createSnowflake() {
        return IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
        );
    }

    private CreateMomentResponse convertToMomentVO(Moment moment, List<String> urls) {
        CreateMomentResponse response = new CreateMomentResponse();
        BeanUtil.copyProperties(moment, response);
        response.setMediaUrls(urls);
        return response;
    }


    @Override
    public Long getMomentOwnerId(Long momentId) {
        Moment moment = this.getById(momentId);

        return moment != null ? moment.getUserId() : null;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteMomentResponse deleteMoment(DeleteMomentRequest request) {
        Moment moment = validateMomentOwnership(request.getMomentId(), request.getUserId());

        deleteAssociatedData(request.getMomentId());
        // 标记为删除状态
        moment.setDeleteTime(new Date());
        moment.setUpdateTime(new Date());

        // 更新朋友圈记录
        QueryWrapper<Moment> queryWrapper = createMomentOwnerQuery(request.getMomentId(), request.getUserId());
        boolean update = this.update(moment, queryWrapper);

        if (!update){
            throw new DatabaseException(ErrorEnum.DATABASE_ERROR.getCode(), ErrorEnum.DATABASE_ERROR.getMessage());
        }


        return new DeleteMomentResponse().setMessage(MomentConstants.DELETE_MOMENT_SUCCESS_MSG);
    }

    private Moment validateMomentOwnership(Long momentId, Long userId) {
        QueryWrapper<Moment> queryWrapper = createMomentOwnerQuery(momentId, userId);
        Moment moment = this.getOne(queryWrapper);

        if (moment == null) {
            throw new UserException(ErrorEnum.DELETE_MOMENT_FAILED_MSG);
        }

        return moment;
    }

    private QueryWrapper<Moment> createMomentOwnerQuery(Long momentId, Long userId) {
        QueryWrapper<Moment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MomentConstants.FIELD_MOMENT_ID, momentId)
                .eq(MomentConstants.FIELD_USER_ID, userId);

        return queryWrapper;
    }

    private void deleteAssociatedData(Long momentId) {
        deleteAssociatedLikes(momentId);
        deleteAssociatedComments(momentId);
    }

    // 删除相关点赞方法保持不变
    private void deleteAssociatedLikes(Long momentId) {
        QueryWrapper<MomentLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MomentConstants.FIELD_MOMENT_ID, momentId);

        momentLikeService.remove(queryWrapper);
    }


    private void deleteAssociatedComments(Long momentId) {
        QueryWrapper<MomentComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MomentConstants.FIELD_MOMENT_ID, momentId);

        momentCommentService.remove(queryWrapper);
    }

}