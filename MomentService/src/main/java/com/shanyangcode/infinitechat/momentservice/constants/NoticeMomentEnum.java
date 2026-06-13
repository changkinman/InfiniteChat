package com.shanyangcode.infinitechat.momentservice.constants;

/**
 * 朋友圈通知类型枚举
 *
 * @author Day
 * @since 1.0.0
 */
public enum NoticeMomentEnum {
    /**
     * 创建朋友圈通知
     */
    CREATE_MOMENT_NOTICE(1, "创建朋友圈通知"),

    /**
     * 创建评论或点赞通知
     */
    CREATE_MOMENT_COMMENT_LIKE_NOTICE(2, "创建评论或点赞通知");

    /**
     * 通知类型值
     */
    private final int value;

    /**
     * 通知类型描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param value 通知类型值
     * @param description 通知类型描述
     */
    NoticeMomentEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取通知类型值
     *
     * @return 通知类型值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取通知类型描述
     *
     * @return 通知类型描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据值获取枚举实例
     *
     * @param value 通知类型值
     * @return 对应的枚举实例，如果不存在则返回null
     */
    public static NoticeMomentEnum getByValue(int value) {
        for (NoticeMomentEnum type : NoticeMomentEnum.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}