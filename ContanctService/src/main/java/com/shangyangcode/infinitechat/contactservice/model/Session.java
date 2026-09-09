package com.shangyangcode.infinitechat.contactservice.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@TableName("session")
@Accessors(chain = true)
public class Session {

    @TableId
    private Long id;

    private String name;

    private Integer type;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private UserSession userSession;
}
