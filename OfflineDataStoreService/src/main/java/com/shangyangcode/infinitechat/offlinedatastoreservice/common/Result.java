package com.shangyangcode.infinitechat.offlinedatastoreservice.common;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Data
@Accessors(chain = true)
public class Result<T> {
    // code 状态码
    private int code;

    // message 信息
    private String message;

    // data 数据
    private T data;

    /***
     * @MethodName toString
     * @Description  序列化
     * @return: java.lang.String
     * @Date 2025/1/15 00:35
     */
    @Override
    public String toString(){
        return JSON.toJSONString(this);
    }

    /***
     * @MethodName ok
     * @Description 正常返回
     * @param: data
     * @return: com.shanyangcode.infinitechat.authenticationservice.common.Result<T>
     * @Date 2025/1/15 00:43
     */
    public static <T> Result<T> ok(T data){
        Result<T> r = new Result<>();

        return r.setCode(HttpStatus.OK.value()).setData(data);
    }

    /***
     * @MethodName ValidError
     * @Description 参数校验错误
     * @param: msg
     * @return: com.shanyangcode.infinitechat.authenticationservice.common.Result<T>
     * @Date 2025/1/17 00:54
     */
    public static <T> Result<T> ValidError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.BAD_REQUEST.value()).setMessage(msg);
    }

    public static <T> Result<T> JWTError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.BAD_REQUEST.value()).setMessage(msg);
    }

    public static <T> Result<T> CodeError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.BAD_REQUEST.value()).setMessage(msg);
    }

    public static <T> Result<T> DataBaseError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMessage(msg);
    }

    public static <T> Result<T> MQError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMessage(msg);
    }

    public static <T> Result<T> UserError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.UNAUTHORIZED.value()).setMessage(msg);
    }
    /***
     * @MethodName ServerError
     * @Description 服务器内部错误
     * @param: msg
     * @return: com.shanyangcode.infinitechat.authenticationservice.common.Result<T>
     * @Date 2025/1/17 00:58
     */
    public static <T> Result<T> ServerError(String msg){
        Result<T> r = new Result<T>();

        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMessage(msg);
    }
}
