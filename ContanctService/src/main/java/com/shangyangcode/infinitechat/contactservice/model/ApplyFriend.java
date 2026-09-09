package com.shangyangcode.infinitechat.contactservice.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@TableName("apply_friend")
@Accessors(chain = true)
public class ApplyFriend {

    @TableId(value = "id")
    private Long id;

    private Long userId;

    private Long targetId;

    private String msg;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
