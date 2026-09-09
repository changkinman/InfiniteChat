package com.shangyangcode.infinitechat.contactservice.data.ApplyList;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class applyFriend {

    private String userUuid;

    private String nickname;

    private String avatar;

    private String msg;

    private Integer status;

    private LocalDateTime time;

    /**
     * 是否是接受者。1 是，0 否
     */
    private Integer isReceiver;
}