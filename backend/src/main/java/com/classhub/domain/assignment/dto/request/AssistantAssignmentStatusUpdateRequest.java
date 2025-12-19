package com.classhub.domain.assignment.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssistantAssignmentStatusUpdateRequest(
        @NotNull(message = "enabled must be provided")
        Boolean enabled
) {
}
