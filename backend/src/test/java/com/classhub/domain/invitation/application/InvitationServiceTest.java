package com.classhub.domain.invitation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.request.InvitationVerifyRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.invitation.dto.response.InvitationVerifyResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.invitation.support.InvitationCodeGenerator;
import com.classhub.domain.member.application.RegisterService;
import com.classhub.domain.member.dto.request.RegisterAssistantByInvitationRequest;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 12, 0);
    private static final Instant NOW_INSTANT = NOW.toInstant(ZoneOffset.UTC);

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;

    @Mock
    private RegisterService registerService;

    @Mock
    private InvitationCodeGenerator invitationCodeGenerator;

    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW_INSTANT, ZoneOffset.UTC);
        invitationService = new InvitationService(
                invitationRepository,
                memberRepository,
                teacherAssistantAssignmentRepository,
                registerService,
                invitationCodeGenerator,
                clock
        );
    }

    @Test
    void createAssistantInvitation_shouldPersistInvitationWithDefaultExpiry() {
        UUID senderId = UUID.randomUUID();
        AssistantInvitationCreateRequest request = new AssistantInvitationCreateRequest("Assistant@Classhub.Com ", null);
        given(invitationRepository.existsByTargetEmailAndStatus("assistant@classhub.com", InvitationStatus.PENDING))
                .willReturn(false);
        given(invitationCodeGenerator.generate()).willReturn("INV-123");

        InvitationResponse response = invitationService.createAssistantInvitation(senderId, request);

        assertThat(response.code()).isEqualTo("INV-123");
        assertThat(response.targetEmail()).isEqualTo("assistant@classhub.com");
        assertThat(response.expiredAt()).isEqualTo(NOW.plusDays(7));

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        Invitation saved = captor.getValue();
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void createAssistantInvitation_shouldThrow_whenPendingExists() {
        AssistantInvitationCreateRequest request = new AssistantInvitationCreateRequest("assistant@classhub.com", null);
        given(invitationRepository.existsByTargetEmailAndStatus("assistant@classhub.com", InvitationStatus.PENDING))
                .willReturn(true);

        assertThatThrownBy(() -> invitationService.createAssistantInvitation(UUID.randomUUID(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void verifyCode_shouldReturnSenderInfo_whenInvitationValid() {
        UUID senderId = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .senderId(senderId)
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code("CODE-1")
                .expiredAt(NOW.plusDays(1))
                .build();
        given(invitationRepository.findByCode("CODE-1")).willReturn(Optional.of(invitation));
        Member sender = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher Kim")
                .phoneNumber("01012345678")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(sender, "id", senderId);
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));

        InvitationVerifyResponse response = invitationService.verifyCode(new InvitationVerifyRequest("CODE-1"));

        assertThat(response.senderId()).isEqualTo(senderId);
        assertThat(response.senderName()).isEqualTo("Teacher Kim");
        assertThat(response.targetEmail()).isEqualTo("assistant@classhub.com");
    }

    @Test
    void verifyCode_shouldThrow_whenInvitationExpired() {
        Invitation invitation = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code("CODE-2")
                .expiredAt(NOW.minusDays(1))
                .build();
        given(invitationRepository.findByCode("CODE-2")).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.verifyCode("CODE-2"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerAssistantViaInvitation_shouldCreateAssignmentAndAcceptInvitation() {
        UUID senderId = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .senderId(senderId)
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code("CODE-3")
                .expiredAt(NOW.plusDays(2))
                .build();
        given(invitationRepository.findByCode("CODE-3")).willReturn(Optional.of(invitation));
        AuthTokens tokens = new AuthTokens(
                UUID.randomUUID(),
                "access",
                NOW.plusMinutes(30),
                "refresh",
                NOW.plusDays(7)
        );
        given(registerService.registerAssistant(any(RegisterMemberRequest.class))).willReturn(tokens);

        RegisterAssistantByInvitationRequest request = new RegisterAssistantByInvitationRequest(
                new RegisterMemberRequest(
                        "ignored@classhub.com",
                        "Classhub!1",
                        "Assistant Kim",
                        "010-0000-1111"
                ),
                "CODE-3"
        );

        AuthTokens result = invitationService.registerAssistantViaInvitation(request);

        assertThat(result).isEqualTo(tokens);
        ArgumentCaptor<TeacherAssistantAssignment> assignmentCaptor =
                ArgumentCaptor.forClass(TeacherAssistantAssignment.class);
        verify(teacherAssistantAssignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getTeacherMemberId()).isEqualTo(senderId);
        assertThat(assignmentCaptor.getValue().getAssistantMemberId()).isEqualTo(tokens.memberId());

        verify(invitationRepository).save(invitation);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void revokeInvitation_shouldUpdateStatus_whenSenderMatches() {
        UUID senderId = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .senderId(senderId)
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code("CODE-4")
                .expiredAt(NOW.plusDays(1))
                .build();
        given(invitationRepository.findByCode("CODE-4")).willReturn(Optional.of(invitation));

        invitationService.revokeInvitation(senderId, " CODE-4 ");

        verify(invitationRepository).save(invitation);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    void revokeInvitation_shouldThrow_whenSenderDifferent() {
        Invitation invitation = Invitation.builder()
                .senderId(UUID.randomUUID())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code("CODE-5")
                .expiredAt(NOW.plusDays(1))
                .build();
        given(invitationRepository.findByCode("CODE-5")).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.revokeInvitation(UUID.randomUUID(), "CODE-5"))
                .isInstanceOf(BusinessException.class);
    }
}
