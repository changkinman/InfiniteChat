package com.shangyangcode.infinitechat.contactservice.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
    public ServiceUnavailableException() {
        super("Netty服务不可用");
    }
}
