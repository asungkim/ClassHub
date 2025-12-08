package com.classhub.domain.invitation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record StudentInvitationCreateRequest(
        @NotEmpty
        List<UUID> studentProfileIds
) {
}
