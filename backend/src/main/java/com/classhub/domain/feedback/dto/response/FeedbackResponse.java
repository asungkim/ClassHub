package com.classhub.domain.feedback.dto.response;

import com.classhub.domain.feedback.model.Feedback;
import com.classhub.domain.feedback.model.FeedbackStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID feedbackId,
        String content,
        FeedbackStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        UUID resolvedByMemberId,
        FeedbackWriterResponse writer
) {

    public static FeedbackResponse from(Feedback feedback, FeedbackWriterResponse writer) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getCreatedAt(),
                feedback.getResolvedAt(),
                feedback.getResolvedByMemberId(),
                writer
        );
    }
}
