package com.classhub.domain.studentprofile.dto.request;

import java.util.UUID;
import org.springframework.util.StringUtils;

public record StudentProfileSearchCondition(
        UUID courseId,
        String name
) {

    public boolean hasCourseFilter() {
        return courseId != null;
    }

    public boolean hasNameFilter() {
        return StringUtils.hasText(name);
    }
}
