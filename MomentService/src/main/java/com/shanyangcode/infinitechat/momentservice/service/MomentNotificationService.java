package com.shanyangcode.infinitechat.momentservice.service;

import java.util.List;

/**
 * 朋友圈通知服务接口
 * 负责处理朋友圈相关的通知发送功能
 */
public interface MomentNotificationService {

    /**
     * 发送朋友圈创建通知
     *
     * @param senderUserId 发送者用户ID
     * @param momentId 朋友圈ID
     * @param receiverUserIds 接收者用户ID列表
     * @param avatar 用户头像
     * @throws Exception 发送通知过程中可能发生的异常
     */
    void sendMomentCreationNotification(Long senderUserId, String avatar, Long momentId, List<Long> receiverUserIds) throws Exception;

    /**
     * 发送朋友圈点赞或评论通知
     *
     * @param senderUserId 发送者用户ID
     * @param momentId 朋友圈ID
     * @param receiverUserIds 接收者用户ID列表
     * @throws Exception 发送通知过程中可能发生的异常
     */
    void sendInteractionNotification(Long senderUserId, Long momentId, List<Long> receiverUserIds) throws Exception;
}