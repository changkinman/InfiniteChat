package com.shangyangcode.infinitechat.contactservice.constants;

import lombok.Getter;

@Getter
public enum ErrorEnum {
    // 400 01 02 03...
    // 500 01 02 03....

    SUCCESS(200, "请求成功"),
    OPTION_ERROR(40001, "操作失败"),

    NO_USER_ERROR(40004, "用户不存在"),
    USER_STATUS_ERROR(40006, "用户状态异常"),
    DELETE_MOMENT_FAILED_MSG(40005, "删除失败, 朋友圈不存在"),

    SYSTEM_ERROR(50000, "系统内部异常"),
    SERVICE_UNAVAILABLE(50001, "实时通信服务不可用"),
    MESSAGE_SEND_FAILURE(50002, "消息发送失败"),
    UPDATE_AVATAR_ERROR(50011, "更新头像失败"),
    ERROR_UPLOAD_FAILED(50012,"上传图片失败" ),
    DATABASE_ERROR(50013, "数据库异常"),

    //群组异常
    //退出群聊失败，关联记录不存在
    GROUP_EXIT_FAILED(400021, "退出群聊失败，关联记录不存在"),
    //用户不在该群聊中
    GROUP_USER_NOT_IN_GROUP(400020, "用户不在该群聊中"),
    //没有成功踢出任何用户
    KICKED_NO_SUCCESS(400019, "没有成功踢出任何用户"),
    //没有指定要踢出的用户
    KICKED_NO_USER(400018, "没有指定要踢出的用户"),
    //管理员不能踢出其他管理员
    KICKED_IS_ADMIN(400017, "管理员不能踢出其他管理员"),
    //不能踢出群主
    KICKED_IS_OWNER(400016, "不能踢出群主"),
    //部分被踢出者不在该群聊中
    KICKED_NOT_IN_GROUP(400015, "部分被踢出者不在该群聊中"),
    //指定的会话不是群聊
    NOT_GROUP(400014, "指定的会话不是群聊"),
    //群聊会话不存在或已删除
    GROUP_NOT_EXIST(400013, "群聊会话不存在或已删除"),
    //被邀请者列表不能为空
    GROUP_INVITEE_LIST_EMPTY(40007, "被邀请者列表不能为空"),
    //邀请者没有权限邀请用户加入群聊
    GROUP_INVITER_NO_PERMISSION(40008, "邀请者没有权限邀请用户加入群聊"),
    //邀请者不在群聊中
    GROUP_INVITER_NOT_IN_GROUP(40009, "邀请者不在群聊中"),
    //批量插入用户会话关系失败
    GROUP_INSERT_ERROR(40012, "批量插入用户会话关系失败"),
    //没有有效的好友可加入群聊
    NO_FRIEND_ERROR(40011, "没有有效的好友可加入群聊");


    private final int code;
    private final String message;

    ErrorEnum(int code, String message) {
        this.code = code;

        this.message = message;
    }
}
