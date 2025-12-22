package com.classhub.global.exception;

import com.classhub.global.response.RsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static com.classhub.global.response.RsCode.BAD_REQUEST;
import static com.classhub.global.response.RsCode.CONCURRENT_UPDATE;
import static com.classhub.global.response.RsCode.INTERNAL_SERVER;
import static com.classhub.global.response.RsCode.FORBIDDEN;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String EXCEPTION_FORMAT = "[EXCEPTION]                   -----> ";
    private static final String EXCEPTION_MESSAGE_FORMAT = "[EXCEPTION] EXCEPTION_MESSAGE -----> [{}]";
    private static final String EXCEPTION_TYPE_FORMAT = "[EXCEPTION] EXCEPTION_TYPE    -----> [{}]";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RsData<?> handleValidationException(MethodArgumentNotValidException ex) {
        logWarn(ex);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> String.format("[%s] %s", error.getField(), error.getDefaultMessage()))
                .orElse(BAD_REQUEST.getMessage());
        return RsData.builder()
                .code(BAD_REQUEST.getCode())
                .message(message)
                .build();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public RsData<?> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        logWarn(ex);
        return RsData.from(FORBIDDEN);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public RsData<?> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex) {
        logWarn(ex);
        return RsData.from(CONCURRENT_UPDATE);
    }

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
