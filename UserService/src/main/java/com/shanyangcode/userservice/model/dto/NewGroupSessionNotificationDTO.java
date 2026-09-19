package com.shanyangcode.userservice.model.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * 新群聊会话通知DTO（消息体body部分）
 *
 * 场景：用户被邀请加入群聊，通知该用户
 *
 * 注意：sessionId、sessionType 等字段已提升至 SystemNotificationMessage 顶层
 * 此DTO仅作为 SystemNotificationMessage.body 的内容
 */
@Data
public class NewGroupSessionNotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 群聊名称
     */
    private String sessionName;

    /**
     * 群聊头像URL
     */
    private String avatar;

    /**
     * 群主用户ID
     */
    private Long creatorId;

    /**
     * 群成员数量
     */
    private Integer membersCount;
}