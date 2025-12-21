package com.classhub.domain.progress.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProgressSliceResponse<T>(
        List<T> items,
        ProgressCursor nextCursor
) {
    public record ProgressCursor(
            UUID id,
            LocalDateTime createdAt
    ) {
    }
}
