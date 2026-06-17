package com.shanyangcode.infinitechat.momentservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 朋友圈
 * @TableName moment
 */
@TableName(value = "moment")
@Data
@Accessors(chain = true)
public class Moment implements Serializable {

    /**
     * 朋友圈id
     */
    @TableId
    private Long momentId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 朋友圈文本内容
     */
    private String text;

    /**
     * 朋友圈媒体
     */
    private String mediaUrl;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除时间
     */
    private Date deleteTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
