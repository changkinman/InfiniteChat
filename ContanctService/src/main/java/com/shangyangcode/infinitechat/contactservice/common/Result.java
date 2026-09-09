package com.shangyangcode.infinitechat.contactservice.common;

import com.shangyangcode.infinitechat.contactservice.constants.ErrorEnum;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Data
@Accessors(chain = true)
public class Result<T> {
    private int code;

    private String msg;

    private T data;

    public static <T> Result<T> OK(T data){
        Result<T> r = new Result<>();

        return r.setCode(HttpStatus.OK.value()).setData(data);
    }

    public static <T> Result<T> ValidError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.BAD_REQUEST.value()).setMsg(msg);
    }

    public static <T> Result<T> DatabaseError(String msg){
        Result<T> r = new Result<>();

        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMsg(msg);
    }

    public static <T> Result<T> ServerError(String msg){
        Result<T> r = new Result<>();

        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMsg(msg);
    }

    public static <T> Result<T> UserError(int code, String msg){
        Result<T> r = new Result<>();

        return r.setCode(code).setMsg(msg);
    }

    public static <T> Result<T> ServiceUnavailableError() {
        Result<T> r = new Result<>();
        return r.setCode(ErrorEnum.SERVICE_UNAVAILABLE.getCode())
                .setMsg(ErrorEnum.SERVICE_UNAVAILABLE.getMessage());
    }

    public static <T> Result<T> GroupError(int code, String msg) {
        Result<T> r = new Result<>();
        return r.setCode(code).setMsg(msg);
    }
}
