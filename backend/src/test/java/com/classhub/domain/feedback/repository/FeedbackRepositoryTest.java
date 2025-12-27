package com.classhub.domain.feedback.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.feedback.model.Feedback;
import com.classhub.domain.feedback.model.FeedbackStatus;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void findAllByStatus_shouldReturnMatchingFeedbacks() {
        UUID memberId = UUID.randomUUID();
        Feedback submitted = feedbackRepository.save(Feedback.builder()
                .memberId(memberId)
                .content("Need dark mode")
                .status(FeedbackStatus.SUBMITTED)
                .build());
        Feedback resolved = Feedback.builder()
                .memberId(memberId)
                .content("Login issue")
                .status(FeedbackStatus.SUBMITTED)
                .build();
        resolved.resolve(UUID.randomUUID(), LocalDateTime.now());
        feedbackRepository.save(resolved);

        var result = feedbackRepository.findAllByStatus(FeedbackStatus.SUBMITTED, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Feedback::getId)
                .contains(submitted.getId())
                .doesNotContain(resolved.getId());
    }

    @Test
    void findAllByMemberId_shouldReturnOnlyMemberFeedbacks() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Feedback ownFeedback = feedbackRepository.save(Feedback.builder()
                .memberId(ownerId)
                .content("Feature request")
                .status(FeedbackStatus.SUBMITTED)
                .build());
        feedbackRepository.save(Feedback.builder()
                .memberId(otherId)
                .content("Other feedback")
                .status(FeedbackStatus.SUBMITTED)
                .build());

        var result = feedbackRepository.findAllByMemberId(ownerId, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Feedback::getId)
                .containsExactly(ownFeedback.getId());
    }

    @Test
    void findAllByMemberIdAndStatus_shouldFilterByMemberAndStatus() {
        UUID memberId = UUID.randomUUID();
        Feedback submitted = feedbackRepository.save(Feedback.builder()
                .memberId(memberId)
                .content("Submit")
                .status(FeedbackStatus.SUBMITTED)
                .build());
        Feedback resolved = Feedback.builder()
                .memberId(memberId)
                .content("Resolved")
                .status(FeedbackStatus.SUBMITTED)
                .build();
        resolved.resolve(UUID.randomUUID(), LocalDateTime.now());
        feedbackRepository.save(resolved);

        var result = feedbackRepository.findAllByMemberIdAndStatus(
                memberId,
                FeedbackStatus.RESOLVED,
                PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Feedback::getId)
                .containsExactly(resolved.getId())
                .doesNotContain(submitted.getId());
    }
}
