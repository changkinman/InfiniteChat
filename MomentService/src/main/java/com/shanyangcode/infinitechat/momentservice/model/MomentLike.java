package com.shanyangcode.infinitechat.momentservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 朋友圈点赞
 * @TableName moment_like
 */
@TableName(value = "moment_like")
@Data
@Accessors(chain = true)
public class MomentLike implements Serializable {

    /**
     * 点赞id
     */
    @TableId
    private Long likeId;

    /**
     * 朋友圈id
     */
    private Long momentId;

    /**
     * 用户id
     */
    private Long userId;

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
