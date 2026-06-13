package com.shanyangcode.infinitechat.momentservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 联系人表
 * @TableName friend
 */
@TableName(value = "friend")
@Data
@Accessors(chain = true)
public class Friend implements Serializable {

    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 好友 ID
     */
    private Long friendId;

    /**
     * 好友状态。1好友，2拉黑，3删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date created_at;

    /**
     * 更新时间
     */
    private Date updated_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
