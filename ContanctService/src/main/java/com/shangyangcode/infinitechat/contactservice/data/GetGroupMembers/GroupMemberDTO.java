package com.shangyangcode.infinitechat.contactservice.data.GetGroupMembers;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 群成员数据传输对象
 */
@Data
@Accessors(chain = true)
public class GroupMemberDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像URL
     */
    private String avatar;
}
