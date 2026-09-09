package com.shangyangcode.infinitechat.contactservice.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@TableName("user_session")
@Accessors(chain = true)
public class UserSession {

    @TableId
    private Long id;

    private Long userId;

    private Long sessionId;

    private Integer role;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
