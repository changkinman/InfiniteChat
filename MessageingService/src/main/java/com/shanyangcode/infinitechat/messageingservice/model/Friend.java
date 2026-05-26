package com.shanyangcode.infinitechat.messageingservice.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@TableName("friend")
@Accessors(chain = true)
public class Friend {

    @TableId
    private Long id;

    private Long userId;

    private Long friendId;

    // 1: 好友, 2: 拉黑, 3: 删除
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
