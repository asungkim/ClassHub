package com.classhub.domain.feedback.dto.response;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import java.util.UUID;

public record FeedbackWriterResponse(
        UUID memberId,
        String name,
        String email,
        String phoneNumber,
        MemberRole role
) {

    public static FeedbackWriterResponse from(Member member) {
        return new FeedbackWriterResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhoneNumber(),
                member.getRole()
        );
    }
}
