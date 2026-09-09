package com.shangyangcode.infinitechat.offlinedatastoreservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName message
 */
@TableName(value ="message")
@Data
public class Message implements Serializable {

    /**
     * 消息id
     */
    private Long messageId;

    /**
     * 回复消息的 id
     */
    private Long replyId;
    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 会话id
     */
    private Long sessionId;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;


    /**
     * 会话类型
     */
    private Integer sessionType;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 修改时间
     */
    private Date updatedAt;

    @TableField(exist = false)
    private RedPacket redPacket;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}