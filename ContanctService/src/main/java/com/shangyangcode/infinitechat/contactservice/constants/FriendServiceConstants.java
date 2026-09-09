package com.shangyangcode.infinitechat.contactservice.constants;

public class FriendServiceConstants {

    // Exception Messages
    public static final String INVALID_USER_ID = "用户ID无效";
    public static final String GET_FRIENDS_FAILED = "获取联系人列表失败";
    public static final String DELETE_FRIEND_FAILED = "删除好友失败";
    public static final String FRIEND_NOT_EXIST = "用户或好友不存在";
    public static final String UNAUTHORIZED_OPERATION = "无权限操作";
    public static final String USER_NOT_EXIST = "该用户不存在";
    public static final String USER_BANNED = "该用户被封禁";
    public static final String USER_DELETED = "该用户已注销";
    public static final String ALREADY_FRIENDS = "已经是好友关系";
    public static final String ADD_FRIEND_FAILED = "添加好友失败";
    public static final String CREATE_SESSION_FAILED = "创建会话失败";
    public static final String CREATE_USER_SESSION_FAILED = "创建用户会话关系失败";
    public static final String PUSH_NOTIFICATION_FAILED = "推送新会话通知失败";

    // Status Codes
    public static final int FRIEND_STATUS_NON_FRIEND = 0;
    public static final int FRIEND_STATUS_ACTIVE = 1;
    public static final int FRIEND_STATUS_BLOCKED = 2;
    public static final int FRIEND_STATUS_DELETED = 3;

    public static final int USER_STATUS_ACTIVE = 1;
    public static final int USER_STATUS_BANNED = 2;
    public static final int USER_STATUS_DELETED = 3;

    public static final int SESSION_TYPE_SINGLE = 1;
    public static final int SESSION_TYPE_GROUP = 2;

    public static final int USER_ROLE_NORMAL = 3;

    // Other Constants
    public static final String EMPTY_STRING = "";
}
