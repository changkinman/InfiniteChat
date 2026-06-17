package com.shanyangcode.infinitechat.momentservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.infinitechat.momentservice.Exception.DatabaseException;
import com.shanyangcode.infinitechat.momentservice.Exception.UserException;
import com.shanyangcode.infinitechat.momentservice.constants.ConfigEnum;
import com.shanyangcode.infinitechat.momentservice.constants.ErrorEnum;
import com.shanyangcode.infinitechat.momentservice.constants.MomentConstants;
import com.shanyangcode.infinitechat.momentservice.data.createComment.CreateCommentRequest;
import com.shanyangcode.infinitechat.momentservice.data.createComment.CreateCommentResponse;
import com.shanyangcode.infinitechat.momentservice.data.createComment.MomentCommentDTO;
import com.shanyangcode.infinitechat.momentservice.data.createComment.MomentCommentVO;
import com.shanyangcode.infinitechat.momentservice.data.deleteComment.DeleteCommentRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteComment.DeleteCommentResponse;
import com.shanyangcode.infinitechat.momentservice.mapper.MomentCommentMapper;
import com.shanyangcode.infinitechat.momentservice.model.MomentComment;
import com.shanyangcode.infinitechat.momentservice.model.User;
import com.shanyangcode.infinitechat.momentservice.service.MomentCommentService;
import com.shanyangcode.infinitechat.momentservice.service.MomentNotificationService;
import com.shanyangcode.infinitechat.momentservice.service.MomentService;
import com.shanyangcode.infinitechat.momentservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

@Service
@Slf4j
public class MomentCommentServiceImpl extends ServiceImpl<MomentCommentMapper, MomentComment> implements MomentCommentService {
    @Autowired
    private UserService userService;

    @Autowired
    @Lazy  // 使用延迟加载避免循环依赖
    private MomentService momentService;

    @Autowired
    private MomentNotificationService notificationService;


    @Override
    public CreateCommentResponse createComment(CreateCommentRequest request) throws Exception {
        MomentCommentVO momentCommentVO = createCommentWithNotification(request.getMomentId(), request.getMomentCommentDTO());

        CreateCommentResponse response = new CreateCommentResponse();
        BeanUtils.copyProperties(momentCommentVO, response);

        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public MomentCommentVO createCommentWithNotification(Long momentId, MomentCommentDTO momentCommentDTO) throws Exception {
        MomentCommentVO commentVO = createComment(momentId, momentCommentDTO);

        Long momentOwnerId = momentService.getMomentOwnerId(momentId);
        ArrayList<Long> receiveIds = new ArrayList<>();

        if (momentOwnerId != null && !momentOwnerId.equals(momentCommentDTO.getUserId())){
            receiveIds.add(momentOwnerId);

            notificationService.sendInteractionNotification(momentCommentDTO.getUserId(), momentId, receiveIds);
        }

        return commentVO;

    }

    public MomentCommentVO createComment(Long momentId, MomentCommentDTO momentCommentDTO){
        MomentComment momentComment = createMomentComment(momentId, momentCommentDTO);
        boolean save = this.save(momentComment);

        if(!save){
            log.error("评论保存失败：朋友圈ID: {}, 用户ID: {}", momentId, momentCommentDTO.getUserId());
            throw new DatabaseException("评论保存失败");
        }

        return buildCommentVO(momentComment, momentCommentDTO);
    }

    private MomentComment createMomentComment(Long momentId, MomentCommentDTO momentCommentDTO){
        MomentComment momentComment = new MomentComment();
        Snowflake snowflake = IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
        );

        momentComment.setCommentId(snowflake.nextId());
        momentComment.setComment(momentCommentDTO.getComment());
        momentComment.setMomentId(momentId);
        momentComment.setUserId(momentCommentDTO.getUserId());
        momentComment.setIsDelete(MomentConstants.NOT_DELETED);

        // 设置父评论ID（如果有）
        if (momentCommentDTO.getParentCommentId() != null) {
            momentComment.setParentCommentId(momentCommentDTO.getParentCommentId());
        }

        return momentComment;
    }

    private MomentCommentVO buildCommentVO(MomentComment momentComment, MomentCommentDTO commentDTO){
        MomentCommentVO momentCommentVO = new MomentCommentVO();
        BeanUtils.copyProperties(momentComment, momentCommentVO);

        User user = userService.getById(commentDTO.getUserId());
        momentCommentVO.setUserName(user.getUserName());

        // 设置父评论信息（如果有）
        if (commentDTO.getParentCommentId() != null) {
            setParentCommentInfo(commentDTO.getParentCommentId(), momentCommentVO);
        }

        return momentCommentVO;
    }

    private void setParentCommentInfo(Long parentCommentId, MomentCommentVO commentVO) {
        MomentComment parentComment = this.getById(parentCommentId);
        if (parentComment != null) {
            Long parentUserId = parentComment.getUserId();
            User parentUser = userService.getById(parentUserId);

            commentVO.setParentUserName(parentUser.getUserName());
            commentVO.setParentCommentId(parentComment.getCommentId());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteCommentResponse deleteComment(DeleteCommentRequest request) {
        deleteComment(request.getMomentId(), request.getMomentId(), request.getUserId());

        return new DeleteCommentResponse().setMessage(MomentConstants.DELETE_COMMENT_SUCCESS_MSG);
    }

    public void deleteComment(Long momentId, Long commentId, Long userId) {
        // 删除当前评论
        deleteCurrentComment(momentId, commentId, userId);

        // 删除子评论
        deleteChildComments(momentId, commentId);
    }

    private void deleteCurrentComment(Long momentId, Long commentId, Long userId) {
        // 查询评论
        MomentComment comment = findComment(momentId, commentId, userId);

        // 没查到评论或不是当前发起人进行的评论，则返回错误信息
        if (comment == null) {
            log.error("删除评论失败：找不到评论记录，朋友圈ID: {}, 评论ID: {}, 用户ID: {}",
                    momentId, commentId, userId);

            throw new UserException(ErrorEnum.DELETE_MOMENT_COMMENT_FAIL_msg);
        }

        // 标记为已删除
        comment.setIsDelete(MomentConstants.DELETED);
        comment.setUpdateTime(new Date());

        // 更新数据库
        QueryWrapper<MomentComment> queryWrapper = createCommentQuery(momentId, commentId, userId);
        this.update(comment, queryWrapper);
    }

    private MomentComment findComment(Long momentId, Long commentId, Long userId) {
        QueryWrapper<MomentComment> queryWrapper = createCommentQuery(momentId, commentId, userId);

        return this.getOne(queryWrapper);
    }

    private QueryWrapper<MomentComment> createCommentQuery(Long momentId, Long commentId, Long userId) {
        QueryWrapper<MomentComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MomentConstants.FIELD_MOMENT_ID, momentId)
                .eq(MomentConstants.FIELD_COMMENT_ID, commentId)
                .eq(MomentConstants.FIELD_USER_ID, userId);

        return queryWrapper;
    }

    private void deleteChildComments(Long momentId, Long parentCommentId) {
        QueryWrapper<MomentComment> commentQueryWrapper = new QueryWrapper<>();
        commentQueryWrapper.eq(MomentConstants.FIELD_MOMENT_ID, momentId)
                .eq(MomentConstants.FIELD_PARENT_COMMENT_ID, parentCommentId);

        MomentComment momentComment = new MomentComment();
        momentComment.setIsDelete(MomentConstants.DELETED);
        momentComment.setUpdateTime(new Date());

        this.update(momentComment, commentQueryWrapper);
    }

}