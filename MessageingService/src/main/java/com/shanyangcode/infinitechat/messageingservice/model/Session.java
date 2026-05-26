package com.shanyangcode.infinitechat.messageingservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 会话表
 * @TableName session
 */
@TableName(value = "session")
@Data
@Accessors(chain = true)
public class Session implements Serializable {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 类别：1 单聊，2 群聊
     */
    private Integer type;

    /**
     * 状态：1 正常，2 删除
     */
    private Integer status;

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
