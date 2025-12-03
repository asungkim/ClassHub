package com.classhub.domain.invitation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.invitation.dto.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.InvitationResponse;
import com.classhub.domain.invitation.dto.StudentInvitationCreateRequest;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import java.util.List;
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
class InvitationServiceTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member assistant;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher")
                        .role(MemberRole.TEACHER)
                        .build()
        );
        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );
    }

    @Test
    @DisplayName("Teacher는 조교 초대를 생성할 수 있다")
    void createAssistantInvitation_success() {
        InvitationResponse response = invitationService.createAssistantInvitation(
                teacher.getId(),
                new AssistantInvitationCreateRequest("newassistant@classhub.com")
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.ASSISTANT);
        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    @DisplayName("Assistant는 학생 초대를 생성할 수 있다")
    void createStudentInvitation_success_assistant() {
        InvitationResponse response = invitationService.createStudentInvitation(
                assistant.getId(),
                new StudentInvitationCreateRequest("student@classhub.com", UUID.randomUUID())
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.STUDENT);
    }

    @Test
    @DisplayName("Teacher도 학생 초대를 생성할 수 있다")
    void createStudentInvitation_success_teacher() {
        InvitationResponse response = invitationService.createStudentInvitation(
                teacher.getId(),
                new StudentInvitationCreateRequest("student2@classhub.com", UUID.randomUUID())
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.STUDENT);
    }

    @Test
    @DisplayName("중복 초대는 생성할 수 없다")
    void createInvitation_duplicate() {
        invitationService.createAssistantInvitation(
                teacher.getId(),
                new AssistantInvitationCreateRequest("dup@classhub.com")
        );

        assertThatThrownBy(() -> invitationService.createAssistantInvitation(
                teacher.getId(),
                new AssistantInvitationCreateRequest("dup@classhub.com")
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("초대 목록을 조회할 수 있다")
    void listInvitations() {
        invitationService.createAssistantInvitation(
                teacher.getId(),
                new AssistantInvitationCreateRequest("one@classhub.com")
        );
        invitationService.createAssistantInvitation(
                teacher.getId(),
                new AssistantInvitationCreateRequest("two@classhub.com")
        );

        List<InvitationResponse> responses =
                invitationService.listInvitations(teacher.getId(), InvitationRole.ASSISTANT, null);

        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("초대를 취소할 수 있다")
    void revokeInvitation() {
        InvitationResponse response = invitationService.createAssistantInvitation(
                teacher.getId(),
                new AssistantInvitationCreateRequest("cancel@classhub.com")
        );

        invitationService.revokeInvitation(teacher.getId(), response.code());

        assertThat(invitationRepository.findByCode(response.code())
                .orElseThrow()
                .getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }
}
