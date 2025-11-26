package com.classhub.global.exception;

import com.classhub.global.response.RsCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final RsCode rsCode;

    public BusinessException(RsCode rsCode) {
        super(rsCode.getMessage());

        this.rsCode = rsCode;
    }
}
