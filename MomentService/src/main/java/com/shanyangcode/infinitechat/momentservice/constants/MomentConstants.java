package com.shanyangcode.infinitechat.momentservice.constants;

/**
 * 朋友圈服务使用的常量
 */
public class MomentConstants {
    // 数据库字段
    /**
     * 朋友圈ID字段名
     */
    public static final String FIELD_MOMENT_ID = "moment_id";

    /**
     * 用户ID字段名
     */
    public static final String FIELD_USER_ID = "user_id";

    /**
     * 创建时间字段名
     */
    public static final String FIELD_CREATE_TIME = "create_time";

    /**
     * 更新时间字段名
     */
    public static final String FIELD_UPDATE_TIME = "update_time";

    /**
     * 删除时间字段名
     */
    public static final String FIELD_DELETE_TIME = "delete_time";

    /**
     * 是否删除字段名
     */
    public static final String FIELD_IS_DELETE = "is_delete";

    /**
     * 朋友圈父评论ID字段名
     */
    public static final String FIELD_PARENT_COMMENT_ID = "parent_comment_id";

    /**
     * 朋友圈点赞ID字段名
     */
    public static final String FIELD_LIKE_ID = "like_id";

    /**
     * 朋友圈评论ID字段名
     */
    public static final String FIELD_COMMENT_ID = "comment_id";

    // 删除状态
    /**
     * 未删除状态值
     */
    public static final int NOT_DELETED = 0;

    /**
     * 已删除状态值
     */
    public static final int DELETED = 1;

    // 查询限制
    /**
     * 限制查询结果为一条的SQL片段
     */
    public static final String LIMIT_ONE = "LIMIT 1";

    // 日期格式
    /**
     * 日期时间格式
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // 文件相关
    /**
     * 文件扩展名分隔符
     */
    public static final String FILE_EXTENSION_SEPARATOR = ".";

    // 日志消息
    /**
     * 上传图片成功日志消息
     */
    public static final String LOG_UPLOAD_SUCCESS = "上传图片成功，图片地址为：{}";

    /**
     * 上传图片失败日志消息
     */
    public static final String LOG_UPLOAD_FAILURE = "上传图片失败，错误信息为：{}";

    /**
     * 保存朋友圈成功日志消息
     */
    public static final String LOG_SAVE_SUCCESS = "保存朋友圈成功，朋友圈信息为：{}";

    /**
     * 查询朋友圈成功日志消息
     */
    public static final String LOG_QUERY_SUCCESS = "查询朋友圈成功，朋友圈信息为：{}";

    // 错误消息
    /**
     * 保存朋友圈失败错误消息
     */
    public static final String ERROR_SAVE_FAILED = "保存朋友圈失败";

    /**
     * 朋友圈不存在错误消息
     */
    public static final String ERROR_MOMENT_NOT_FOUND = "朋友圈不存在或您没有权限删除";

    /**
     * 文件上传失败错误消息
     */
    public static final String ERROR_UPLOAD_FAILED = "文件上传失败";

    /**
     * 删除朋友圈失败提示信息
     */
    public static final String DELETE_MOMENT_FAILED_MSG = "删除失败, 朋友圈不存在";

    /**
     * 删除朋友圈成功提示信息
     */
    public static final String DELETE_MOMENT_SUCCESS_MSG = "朋友圈删除成功！";

    /**
     * 取消点赞失败提示信息
     */
    public static final String DELETE_LIKE_FAILED_MSG = "取消点赞失败, 点赞不存在";

    /**
     * 取消点赞成功提示信息
     */
    public static final String DELETE_LIKE_SUCCESS_MSG = "取消点赞成功！";

    /**
     * 删除评论失败提示信息
     */
    public static final String DELETE_COMMENT_FAILED_MSG = "删除失败, 评论不存在";

    /**
     * 删除评论成功提示信息
     */
    public static final String DELETE_COMMENT_SUCCESS_MSG = "评论删除成功！";

    /**
     * 私有构造函数，防止实例化
     */
    private MomentConstants() {
        // 私有构造函数，防止实例化
    }
}