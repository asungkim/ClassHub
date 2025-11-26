package com.classhub.global.error;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
