package com.shanyangcode.infinitechat.realtimecommunicationservice.excption;

import com.shanyangcode.infinitechat.realtimecommunicationservice.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(value = Throwable.class)
    public Result<?> handleException(Throwable err){
        log.error("未知错误:",  err);

        return Result.ServerError(err.getMessage());
    }

    @ExceptionHandler(value= MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        Map<String,String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError)->{
            errorMap.put(fieldError.getField(),fieldError.getDefaultMessage());
        });

        log.error("数据校验出现问题{}，错误信息为：{}",e.getMessage(),errorMap);

        return Result.ValidError(errorMap.toString());
    }
}
