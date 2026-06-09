package com.shanyangcode.infinitechat.momentservice.Exception;

import com.shanyangcode.infinitechat.momentservice.common.Result;
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

    @ExceptionHandler(value = UserException.class)
    public Result<?> handleUserException(UserException err) {
        log.error("用户错误信息：{}", err.getMessage());

        return Result.UserError(err.getCode(), err.getMessage());
    }

    @ExceptionHandler(value = CodeException.class)
    public Result<?> handleCodeException(CodeException err) {
        log.error("验证码信息错误：{}", err.getMessage());

        return Result.UserError(err.getCode(), err.getMessage());
    }

    @ExceptionHandler(value = DatabaseException.class)
    public Result<?> handleCodeException(DatabaseException err) {
        log.error("数据库错误：{}", err.getMessage());

        return Result.DatabaseError(err.getMessage());
    }


    @ExceptionHandler(value = Throwable.class)
    public Result<?> handleException(Throwable err) {
        log.error("未知错误:", err);

        return Result.ServerError(err.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError) -> {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        });

        log.error("数据校验出现问题{}，错误信息为：{}", e.getMessage(), errorMap);

        return Result.ValidError(errorMap.toString());
    }

    @ExceptionHandler(value = ServiceUnavailableException.class)
    public Result<?> handleServiceUnavailableException(ServiceUnavailableException e) {
        log.error("Netty服务不可用错误：{}", e.getMessage());
        return Result.ServiceUnavailableError();
    }

    @ExceptionHandler(MessageSendFailureException.class)
    public Result<?> handleMessageSendFailure(MessageSendFailureException ex) {
        log.error("消息发送失败，原始请求: {}", ex.getRequestPayload(), ex);
        return Result.ServerError(ex.getError().getMessage())
                .setCode(ex.getError().getCode())
                .setData(ex.getRequestPayload()); // 可选返回失败请求数据
    }

    // 新增文件上传异常处理
    @ExceptionHandler(value = FileUploadException.class)
    public Result<?> handleFileUploadException(FileUploadException err) {
        log.error("文件上传操作异常：{}", err.getMessage());
        return Result.FileUploadError(err.getCode(), err.getMessage());
    }

    // 新增朋友圈异常处理
    @ExceptionHandler(value = MomentException.class)
    public Result<?> handleMomentException(MomentException err) {
        log.error("朋友圈操作异常：{}", err.getMessage());
        return Result.MomentError(err.getCode(), err.getMessage());
    }
}
