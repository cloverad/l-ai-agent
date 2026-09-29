package com.ljr.common;

import lombok.Data;

@Data
public class BaseResponse<T> {
    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }
}
