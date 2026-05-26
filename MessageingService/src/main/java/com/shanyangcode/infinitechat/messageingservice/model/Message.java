package com.shanyangcode.infinitechat.messageingservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName message
 */
@TableName(value = "message")
@Data
@Accessors(chain = true)
public class Message implements Serializable {

    /**
     * 消息id
     */
    @TableId
    private Long messageId;

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 会话id
     */
    private Long sessionId;

    /**
     * 消息类型。1文本消息，2图片消息，3文件消息，4视频消息，5红包，6表情包
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 引用消息id
     */
    private Long replyId;

    /**
     * 会话类型。1单聊，2群聊
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
    private static final long serialVersionUID = 1L;
}
