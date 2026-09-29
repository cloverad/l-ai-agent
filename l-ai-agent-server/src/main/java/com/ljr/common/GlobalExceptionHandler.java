package com.ljr.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return BaseResponse.error(40000, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return BaseResponse.error(50000, "系统异常: " + e.getMessage());
    }
}
