package com.shangyangcode.infinitechat.contactservice.exception;

import com.shangyangcode.infinitechat.contactservice.constants.ErrorEnum;
import lombok.Getter;

/**
 * 数据库操作失败时抛出的异常
 */
@Getter
public class DatabaseException extends RuntimeException {
    private final int code;

    /**
     * 使用默认错误码构造数据库异常
     *
     * @param message 错误消息
     */
    public DatabaseException(String message) {
        super(message);
        this.code = ErrorEnum.SYSTEM_ERROR.getCode(); // 默认系统错误码
    }

    /**
     * 使用指定错误码构造数据库异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public DatabaseException(int code, String message) {
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