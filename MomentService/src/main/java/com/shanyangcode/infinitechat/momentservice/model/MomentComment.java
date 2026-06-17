package com.shanyangcode.infinitechat.momentservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 朋友圈评论
 * @TableName moment_comment
 */
@TableName(value = "moment_comment")
@Data
@Accessors(chain = true)
public class MomentComment implements Serializable {

    /**
     * 评论id
     */
    @TableId
    private Long commentId;

    /**
     * 朋友圈id
     */
    private Long momentId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 回复的父评论ID
     */
    private Long parentCommentId;

    /**
     * 评论内容
     */
    private String comment;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 0 未删除  1 删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
