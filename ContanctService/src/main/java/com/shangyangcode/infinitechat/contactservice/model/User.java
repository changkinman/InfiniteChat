package com.shangyangcode.infinitechat.contactservice.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@TableName("user")
@Accessors(chain = true)
public class User {

    @TableId
    private Long userId;

    private String userName;

    private String password;

    private String email;

    private String phone;

    private String avatar;

    private String signature;

    private Integer gender;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
