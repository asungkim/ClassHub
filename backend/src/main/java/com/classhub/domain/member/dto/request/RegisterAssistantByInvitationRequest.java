package com.classhub.domain.member.dto.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAssistantByInvitationRequest(
        @Valid
        @JsonUnwrapped
        RegisterMemberRequest member,

        @NotBlank
        @Size(min = 8, max = 64)
        String code
) {

    public RegisterMemberRequest memberRequest() {
        return member;
    }

    public String normalizedEmail() {
        return member.normalizedEmail();
    }

    public String normalizedPhoneNumber() {
        return member.normalizedPhoneNumber();
    }

    public String trimmedCode() {
        return code.trim();
    }
}
