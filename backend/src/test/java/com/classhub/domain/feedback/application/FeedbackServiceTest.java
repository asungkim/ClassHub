package com.classhub.domain.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.classhub.domain.feedback.dto.request.FeedbackCreateRequest;
import com.classhub.domain.feedback.dto.response.FeedbackResponse;
import com.classhub.domain.feedback.model.Feedback;
import com.classhub.domain.feedback.model.FeedbackStatus;
import com.classhub.domain.feedback.repository.FeedbackRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private UUID memberId;
    private Member member;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        member = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher Kim")
                .phoneNumber("01012345678")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
    }

    @Test
    void createFeedback_shouldTrimContentAndReturnResponse() {
        UUID feedbackId = UUID.randomUUID();
        FeedbackCreateRequest request = new FeedbackCreateRequest("  Great service  ");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(feedbackRepository.save(any(Feedback.class))).willAnswer(invocation -> {
            Feedback saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", feedbackId);
            return saved;
        });

        FeedbackResponse response = feedbackService.createFeedback(memberId, request);

        assertThat(response.feedbackId()).isEqualTo(feedbackId);
        assertThat(response.content()).isEqualTo("Great service");
        assertThat(response.status()).isEqualTo(FeedbackStatus.SUBMITTED);
        assertThat(response.writer().email()).isEqualTo("teacher@classhub.com");
        assertThat(response.resolvedAt()).isNull();
        assertThat(response.resolvedByMemberId()).isNull();
    }

    @Test
    void createFeedback_shouldRejectBlankContent() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("   ");

        assertThatThrownBy(() -> feedbackService.createFeedback(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }

    @Test
    void resolveFeedback_shouldUpdateStatusAndMetadata() {
        UUID feedbackId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Feedback feedback = Feedback.builder()
                .memberId(memberId)
                .content("Need calendar view")
                .status(FeedbackStatus.SUBMITTED)
                .build();
        ReflectionTestUtils.setField(feedback, "id", feedbackId);
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));
        given(feedbackRepository.save(feedback)).willReturn(feedback);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        FeedbackResponse response = feedbackService.resolveFeedback(feedbackId, adminId);

        assertThat(response.status()).isEqualTo(FeedbackStatus.RESOLVED);
        assertThat(response.resolvedByMemberId()).isEqualTo(adminId);
        assertThat(response.resolvedAt()).isNotNull();
        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.RESOLVED);
        assertThat(feedback.getResolvedAt()).isNotNull();
    }

    @Test
    void resolveFeedback_shouldThrowWhenAlreadyResolved() {
        UUID feedbackId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Feedback feedback = Feedback.builder()
                .memberId(memberId)
                .content("Already resolved")
                .status(FeedbackStatus.SUBMITTED)
                .build();
        ReflectionTestUtils.setField(feedback, "id", feedbackId);
        feedback.resolve(adminId, LocalDateTime.now());
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));

        assertThatThrownBy(() -> feedbackService.resolveFeedback(feedbackId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FEEDBACK_ALREADY_RESOLVED);
    }
}
