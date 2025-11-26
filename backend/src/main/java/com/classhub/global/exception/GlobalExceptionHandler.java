package com.classhub.global.exception;

import com.classhub.global.response.RsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.classhub.global.response.RsCode.INTERNAL_SERVER;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String EXCEPTION_FORMAT = "[EXCEPTION]                   -----> ";
    private static final String EXCEPTION_MESSAGE_FORMAT = "[EXCEPTION] EXCEPTION_MESSAGE -----> [{}]";
    private static final String EXCEPTION_TYPE_FORMAT = "[EXCEPTION] EXCEPTION_TYPE    -----> [{}]";

    @ExceptionHandler(Exception.class)
    public RsData<?> handleAllExceptions(Exception e) {
        logError(e);
        return RsData.from(INTERNAL_SERVER);
    }

    @ExceptionHandler(BusinessException.class)
    public RsData<?> handleBusinessException(
            final BusinessException ex
    ) {
        logWarn(ex);
        return RsData.from(ex.getRsCode());
    }

    private void logError(Exception e) {
        log.error(EXCEPTION_TYPE_FORMAT, e.getClass().getSimpleName());
        log.error(EXCEPTION_MESSAGE_FORMAT, e.getMessage());
        log.error(EXCEPTION_FORMAT, e);
    }

    private void logWarn(Exception e) {
        log.warn(EXCEPTION_TYPE_FORMAT, e.getClass().getSimpleName());
        log.warn(EXCEPTION_MESSAGE_FORMAT, e.getMessage());
    }
}
