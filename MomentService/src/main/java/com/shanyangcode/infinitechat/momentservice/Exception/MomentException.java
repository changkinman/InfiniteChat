package com.shanyangcode.infinitechat.momentservice.Exception;

import com.shanyangcode.infinitechat.momentservice.constants.ErrorEnum;

/**
 * 朋友圈操作失败时抛出的异常
 */
public class MomentException extends RuntimeException {
    private final int code;

    /**
     * 使用默认错误码构造朋友圈异常
     *
     * @param message 错误消息
     */
    public MomentException(String message) {
        super(message);
        this.code = ErrorEnum.SYSTEM_ERROR.getCode(); // 默认系统错误码
    }

    /**
     * 使用指定错误码构造朋友圈异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public MomentException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}