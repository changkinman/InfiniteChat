package com.shangyangcode.infinitechat.contactservice.exception;

import com.shangyangcode.infinitechat.contactservice.constants.ErrorEnum;
import lombok.Getter;

/**
 * 消息发送失败异常（HTTP 500）
 * 适用场景：实时通信消息推送失败、OkHttp请求失败等场景
 */
@Getter
public class MessageSendFailureException extends RuntimeException {

    private final ErrorEnum error;
    // 新增获取失败请求数据的方法
    private final String requestPayload; // 新增原始请求数据字段

    public MessageSendFailureException(ErrorEnum error, String requestPayload) {
        super(error.getMessage());
        this.error = error;
        this.requestPayload = requestPayload;
    }

    public MessageSendFailureException(ErrorEnum error, String requestPayload, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
        this.requestPayload = requestPayload;
    }

}
