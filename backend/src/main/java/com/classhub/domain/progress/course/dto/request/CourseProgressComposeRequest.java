package com.classhub.domain.progress.course.dto.request;

import com.classhub.domain.progress.personal.dto.request.PersonalProgressComposeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CourseProgressComposeRequest(
        @NotNull @Valid CourseProgressCreateRequest courseProgress,
        @NotNull List<@Valid PersonalProgressComposeRequest> personalProgressList
) {
}
