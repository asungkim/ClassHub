package com.classhub.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.auth.dto.request.InvitationRegisterRequest;
import com.classhub.domain.auth.dto.request.InvitationVerifyRequest;
import com.classhub.domain.auth.dto.response.InvitationVerifyResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InvitationAuthServiceTest {

    @Autowired
    private InvitationAuthService invitationAuthService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;

    @BeforeEach
    void setup() {
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Kim")
                        .role(MemberRole.TEACHER)
                        .build()
        );
    }

    @Test
    @DisplayName("유효한 초대 코드를 검증하면 inviter와 role 정보를 반환한다")
    void verifyInvitation_success() {
        Invitation invitation = createInvitation(teacher.getId(), "assistant@classhub.com", InvitationRole.ASSISTANT);

        InvitationVerifyResponse response = invitationAuthService.verify(
                new InvitationVerifyRequest(invitation.getCode())
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.ASSISTANT.name());
        assertThat(response.inviterId()).isEqualTo(teacher.getId());
    }

    @Test
    @DisplayName("만료된 초대 코드는 INVALID_INVITATION 예외를 던진다")
    void verifyInvitation_expired() {
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .senderId(teacher.getId())
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code("expired-code")
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .status(InvitationStatus.PENDING)
                .build());

        assertThatThrownBy(() -> invitationAuthService.verify(new InvitationVerifyRequest(invitation.getCode())))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.INVALID_INVITATION.getMessage());
    }

    @Test
    @DisplayName("초대 기반 회원가입으로 Assistant 계정을 생성한다")
    void registerInvited_assistant() {
        Invitation invitation = createInvitation(teacher.getId(), "assistant@classhub.com", InvitationRole.ASSISTANT);

        var response = invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "assistant@classhub.com", "Classhub!1", "조교")
        );

        Member saved = memberRepository.findByEmail("assistant@classhub.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.ASSISTANT);
        assertThat(saved.getTeacherId()).isEqualTo(teacher.getId());
        assertThat(response.accessToken()).isNotBlank();
        assertThat(invitationRepository.findById(invitation.getId()).orElseThrow().getStatus())
                .isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("잘못된 초대 코드로 회원가입을 시도하면 INVALID_INVITATION")
    void registerInvited_invalidCode() {
        assertThatThrownBy(() -> invitationAuthService.registerInvited(
                new InvitationRegisterRequest("invalid", "user@classhub.com", "Classhub!1", "사용자")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.INVALID_INVITATION.getMessage());
    }

    private Invitation createInvitation(UUID senderId, String email, InvitationRole role) {
        return invitationRepository.save(Invitation.builder()
                .senderId(senderId)
                .targetEmail(email)
                .inviteeRole(role)
                .code(UUID.randomUUID().toString())
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
                .status(InvitationStatus.PENDING)
                .build());
    }
}
