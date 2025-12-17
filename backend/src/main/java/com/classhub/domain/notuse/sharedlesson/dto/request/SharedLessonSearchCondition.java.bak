package com.classhub.domain.sharedlesson.dto.request;

import java.time.LocalDate;

public record SharedLessonSearchCondition(
        LocalDate from,
        LocalDate to
) {

    public boolean hasDateFilter() {
        return from != null || to != null;
    }
}
