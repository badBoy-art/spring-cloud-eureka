package com.example.response;

import lombok.Builder;
import lombok.Getter;

/**
 * @author badBoy
 * @create 2019-09-12
 */
@Builder
@Getter
public class BaseWebResponse <T> {
    private int errorCode;
    private T data;

    public static BaseWebResponse successNoData() {
        return BaseWebResponse.builder()
                .build();
    }

    public static <T> BaseWebResponse<T> successWithData(T data) {
        return BaseWebResponse.<T>builder()
                .data(data)
                .build();
    }

    public static BaseWebResponse error(int errorCode) {
        return BaseWebResponse.builder()
                .errorCode(errorCode)
                .build();
    }
}
