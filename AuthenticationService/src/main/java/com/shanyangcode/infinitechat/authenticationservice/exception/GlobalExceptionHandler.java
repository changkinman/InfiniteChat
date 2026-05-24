package com.shanyangcode.infinitechat.authenticationservice.exception;

import com.shanyangcode.infinitechat.authenticationservice.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = UserException.class)
    public Result<?> handleUserException(UserException err){
        log.error("用户错误信息：{}",  err.getMessage());

        return Result.UserError(err.getCode(), err.getMessage());
    }

    @ExceptionHandler(value = CodeException.class)
    public Result<?> handleCodeException(CodeException err){
        log.error("验证码信息错误：{}",  err.getMessage());

        return Result.UserError(err.getCode(), err.getMessage());
    }

    @ExceptionHandler(value = DatabaseException.class)
    public Result<?> handleCodeException(DatabaseException err){
        log.error("数据库错误：{}",  err.getMessage());

        return Result.DatabaseError(err.getMessage());
    }


    @ExceptionHandler(value = Throwable.class)
    public Result<?> handleException(Throwable err){
        log.error("未知错误:",  err);

        return Result.ServerError(err.getMessage());
    }
}
