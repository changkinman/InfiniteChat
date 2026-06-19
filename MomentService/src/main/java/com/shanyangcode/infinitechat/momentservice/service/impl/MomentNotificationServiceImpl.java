package com.shanyangcode.infinitechat.momentservice.service.impl;

import com.shanyangcode.infinitechat.momentservice.constants.NoticeMomentEnum;
import com.shanyangcode.infinitechat.momentservice.model.vo.MomentRTCVO;
import com.shanyangcode.infinitechat.momentservice.service.MomentNotificationService;
import com.shanyangcode.infinitechat.momentservice.utils.SendOkHttpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 朋友圈通知服务实现类
 * 处理朋友圈相关的通知发送逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MomentNotificationServiceImpl implements MomentNotificationService {

    private final SendOkHttpRequest sendOkHttpRequest;

    /**
     * 发送朋友圈创建通知
     * 向用户的所有好友发送新朋友圈创建的通知
     *
     * @param senderUserId 发送者用户ID
     * @param momentId 朋友圈ID
     * @param receiverUserIds 接收者用户ID列表
     * @param avatar 用户头像
     * @throws Exception 发送通知过程中可能发生的异常
     */
    @Override
    public void sendMomentCreationNotification(Long senderUserId, String avatar, Long momentId, List<Long> receiverUserIds) throws Exception {
        log.debug("开始发送朋友圈创建通知，用户ID: {}, 朋友圈ID: {}", senderUserId, momentId);

        // 创建通知对象
        MomentRTCVO momentRTCVO = new MomentRTCVO();
        momentRTCVO.setNoticeType(NoticeMomentEnum.CREATE_MOMENT_NOTICE.getValue());
        momentRTCVO.setAvatar(avatar);
        momentRTCVO.setReceiveUserIds(receiverUserIds);



        // 发送通知
        sendOkHttpRequest.sendNotification(
                momentRTCVO,
                senderUserId,
                NoticeMomentEnum.CREATE_MOMENT_NOTICE.getValue(),
                momentId
        );

        log.debug("朋友圈创建通知发送完成");
    }

    /**
     * 发送朋友圈点赞或评论通知
     * 向朋友圈创建者发送有人点赞或评论的通知
     *
     * @param senderUserId 发送者用户ID
     * @param momentId 朋友圈ID
     * @param receiverUserIds 接收者用户ID列表
     * @throws Exception 发送通知过程中可能发生的异常
     */
    @Override
    public void sendInteractionNotification(Long senderUserId, Long momentId, List<Long> receiverUserIds) throws Exception {

        // 无接收者则不发送
        if (receiverUserIds == null || receiverUserIds.isEmpty()) {
            log.info("没有通知接收者，不发送通知");
            return;
        }

        // 创建通知对象
        MomentRTCVO momentRTCVO = new MomentRTCVO();
        momentRTCVO.setNoticeType(NoticeMomentEnum.CREATE_MOMENT_COMMENT_LIKE_NOTICE.getValue());
        momentRTCVO.setReceiveUserIds(receiverUserIds);

        // 发送通知
        sendOkHttpRequest.sendNotification(
                momentRTCVO,
                senderUserId,
                NoticeMomentEnum.CREATE_MOMENT_COMMENT_LIKE_NOTICE.getValue(),
                momentId
        );

        log.debug("点赞或评论通知发送完成");
    }
}