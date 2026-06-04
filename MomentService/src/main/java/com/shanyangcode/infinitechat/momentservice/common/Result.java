package com.shanyangcode.infinitechat.momentservice.common;

import com.shanyangcode.infinitechat.momentservice.constants.ErrorEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

import static com.shanyangcode.infinitechat.momentservice.constants.ConfigEnum.REQUEST_SUCCESSFUL;

/**
 * 统一响应结果类
 *
 * @param <T> 响应数据的类型
 * @author Day
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;


    /**
     * 创建成功响应结果
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> OK(T data) {
        return new Result<>(200, REQUEST_SUCCESSFUL.getValue(), data);
    }

    /**
     * 创建请求参数验证错误的响应结果
     *
     * @param msg 错误消息
     * @param <T> 响应数据类型
     * @return 验证错误的响应结果
     */
    public static <T> Result<T> ValidError(String msg) {
        Result<T> r = new Result<>();
        return r.setCode(HttpStatus.BAD_REQUEST.value()).setMsg(msg);
    }

    /**
     * 创建数据库操作错误的响应结果
     *
     * @param msg 错误消息
     * @param <T> 响应数据类型
     * @return 数据库错误的响应结果
     */
    public static <T> Result<T> DatabaseError(String msg) {
        return ServerError(msg);
    }

    /**
     * 创建服务器内部错误的响应结果
     *
     * @param msg 错误消息
     * @param <T> 响应数据类型
     * @return 服务器错误的响应结果
     */
    public static <T> Result<T> ServerError(String msg) {
        Result<T> r = new Result<>();
        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMsg(msg);
    }

    /**
     * 创建自定义用户错误的响应结果
     *
     * @param code 错误代码
     * @param msg  错误消息
     * @param <T>  响应数据类型
     * @return 自定义错误的响应结果
     */
    public static <T> Result<T> UserError(int code, String msg) {
        Result<T> r = new Result<>();
        return r.setCode(code).setMsg(msg);
    }

    /**
     * 创建实时通信服务不可用的响应结果
     *
     * @param <T>  响应数据类型
     * @return 自定义错误的响应结果
     */
    public static <T> Result<T> ServiceUnavailableError() {
        Result<T> r = new Result<>();
        return r.setCode(ErrorEnum.SERVICE_UNAVAILABLE.getCode())
                .setMsg(ErrorEnum.SERVICE_UNAVAILABLE.getMessage());
    }

    public static Result<?> FileUploadError(int code, String message) {
        Result<?> r = new Result<>();
        return r.setCode(code).setMsg(message);
    }

    public static Result<?> MomentError(int code, String message) {
        Result<?> r = new Result<>();
        return r.setCode(code).setMsg(message);
    }
}