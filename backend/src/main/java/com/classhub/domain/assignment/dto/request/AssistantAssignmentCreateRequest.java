package com.classhub.domain.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssistantAssignmentCreateRequest(
        @NotNull UUID assistantMemberId
) {
}
