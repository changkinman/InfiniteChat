package com.shangyangcode.infinitechat.contactservice.constants;

/**
 * 好友申请相关常量
 */
public class FriendRequestConstants {

    // Redis Key 前缀
    public static final String FRIEND_REQUEST_KEY_PREFIX = "friend_request:";

    // 过期时间，单位为秒（72小时）
    public static final long FRIEND_REQUEST_EXPIRATION_SECONDS = 72 * 60 * 60;

    // Redis中表示激活状态的值
    public static final String ACTIVE_STATUS = "active";

    // 申请状态字段名
    public static final String STATUS = "status";

    // 申请通过状态
    public static final Integer STATUS_ACCEPTED = 1;

    // 申请过期状态
    public static final Integer STATUS_EXPIRED = 4;

    // 用户ID字段名
    public static final String USER_ID = "user_id";

    // 目标ID字段名
    public static final String TARGET_ID = "target_id";

    // 用户名字段名
    public static final String USER_NAME = "user_name";

    // 用户ID字符串字段名（用于LIKE查询）
    public static final String USER_ID_STR = "user_id";

    // 电话字段名
    public static final String PHONE = "phone";

    // 创建时间字段名
    public static final String CREATED_AT = "created_at";

    // 接收者标识（否）
    public static final int IS_RECEIVER_NO = 0;

    // 接收者标识（是）
    public static final int IS_RECEIVER_YES = 1;

    // 日志信息
    public static final String PUSH_FAILURE_LOG = "推送好友申请消息失败，{}";
    public static final String APPLICANT_NOT_FOUND = "好友申请发送者不存在";
    public static final String FRIEND_ALREADY_ADDED = "已添加好友";
}
