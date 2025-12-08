package com.classhub.domain.invitation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.request.StudentInvitationCreateRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.invitation.dto.response.StudentCandidateResponse;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    private static final List<InvitationStatus> PENDING_STATUSES = List.of(InvitationStatus.PENDING);

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member assistant;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        studentProfileRepository.deleteAll();
        invitationRepository.deleteAll();
        memberRepository.deleteAll();

        courseId = UUID.randomUUID();

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
        // Given: Assistant에게 할당된 StudentProfile
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("학생A")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );

        InvitationResponse response = invitationService.createStudentInvitation(
                assistant.getId(),
                new StudentInvitationCreateRequest(List.of(profile.getId()))
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.STUDENT);
    }

    @Test
    @DisplayName("Teacher도 학생 초대를 생성할 수 있다")
    void createStudentInvitation_success_teacher() {
        // Given: Teacher의 StudentProfile
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("학생B")
                        .phoneNumber("010-2222-2222")
                        .parentPhone("010-8888-8888")
                        .schoolName("서울고")
                        .grade("고2")
                        .age(17)
                        .active(true)
                        .build()
        );

        InvitationResponse response = invitationService.createStudentInvitation(
                teacher.getId(),
                new StudentInvitationCreateRequest(List.of(profile.getId()))
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

    @Test
    @DisplayName("Teacher는 자신의 StudentProfile 중 초대되지 않은 후보를 조회할 수 있다")
    void findStudentCandidates_teacher_success() {
        // Given: Teacher의 StudentProfile 3개 생성 (memberId=null, active=true)
        StudentProfile profile1 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("김철수")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );
        StudentProfile profile2 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("이영희")
                        .phoneNumber("010-2222-2222")
                        .parentPhone("010-8888-8888")
                        .schoolName("서울고")
                        .grade("고2")
                        .age(17)
                        .active(true)
                        .build()
        );
        // profile3은 이미 초대된 상태 (PENDING 초대 존재)
        StudentProfile profile3 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("박민수")
                        .phoneNumber("010-3333-3333")
                        .parentPhone("010-7777-7777")
                        .schoolName("서울고")
                        .grade("고1")
                        .age(16)
                        .active(true)
                        .build()
        );
        invitationRepository.save(
                Invitation.builder()
                        .senderId(teacher.getId())
                        .studentProfileId(profile3.getId())
                        .inviteeRole(InvitationRole.STUDENT)
                        .status(InvitationStatus.PENDING)
                        .code(UUID.randomUUID().toString())
                        .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                        .build()
        );

        // When
        List<StudentCandidateResponse> candidates = invitationService.findStudentCandidates(teacher.getId(), null);

        // Then: profile1, profile2만 반환 (profile3은 PENDING 초대 있어서 제외)
        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(StudentCandidateResponse::name)
                .containsExactlyInAnyOrder("김철수", "이영희");
    }

    @Test
    @DisplayName("Assistant는 자신에게 할당된 StudentProfile 중 초대되지 않은 후보를 조회할 수 있다")
    void findStudentCandidates_assistant_success() {
        // Given: Assistant에게 할당된 StudentProfile 2개
        StudentProfile profile1 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("학생A")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );
        // 다른 조교에게 할당된 프로필 (조회되지 않아야 함)
        UUID otherAssistantId = UUID.randomUUID();
        studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(otherAssistantId)
                        .name("학생B")
                        .phoneNumber("010-2222-2222")
                        .parentPhone("010-8888-8888")
                        .schoolName("서울고")
                        .grade("고2")
                        .age(17)
                        .active(true)
                        .build()
        );

        // When
        List<StudentCandidateResponse> candidates = invitationService.findStudentCandidates(assistant.getId(), null);

        // Then: assistant에게 할당된 profile1만 반환
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).name()).isEqualTo("학생A");
    }

    @Test
    @DisplayName("학생 초대 후보 조회 시 이름으로 필터링할 수 있다")
    void findStudentCandidates_withNameFilter() {
        // Given
        studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("김철수")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );
        studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("이영희")
                        .phoneNumber("010-2222-2222")
                        .parentPhone("010-8888-8888")
                        .schoolName("서울고")
                        .grade("고2")
                        .age(17)
                        .active(true)
                        .build()
        );

        // When: "철수" 검색
        List<StudentCandidateResponse> candidates = invitationService.findStudentCandidates(teacher.getId(), "철수");

        // Then
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).name()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("memberId가 있는 StudentProfile은 초대 후보에서 제외된다")
    void findStudentCandidates_excludeMemberIdNotNull() {
        // Given: memberId가 있는 프로필 (이미 회원가입 완료)
        studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .memberId(UUID.randomUUID()) // memberId 존재
                        .name("가입완료학생")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );

        // When
        List<StudentCandidateResponse> candidates = invitationService.findStudentCandidates(teacher.getId(), null);

        // Then: 빈 리스트 반환
        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("active=false인 StudentProfile은 초대 후보에서 제외된다")
    void findStudentCandidates_excludeInactive() {
        // Given: active=false인 프로필
        studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("비활성학생")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(false) // 비활성
                        .build()
        );

        // When
        List<StudentCandidateResponse> candidates = invitationService.findStudentCandidates(teacher.getId(), null);

        // Then: 빈 리스트 반환
        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Teacher는 여러 학생 프로필에 대해 일괄 초대를 생성할 수 있다")
    void createStudentInvitations_teacher_success() {
        // Given: Teacher의 StudentProfile 3개 생성
        StudentProfile profile1 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("김철수")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );
        StudentProfile profile2 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("이영희")
                        .phoneNumber("010-2222-2222")
                        .parentPhone("010-8888-8888")
                        .schoolName("서울고")
                        .grade("고2")
                        .age(17)
                        .active(true)
                        .build()
        );
        StudentProfile profile3 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("박민수")
                        .phoneNumber("010-3333-3333")
                        .parentPhone("010-7777-7777")
                        .schoolName("서울고")
                        .grade("고1")
                        .age(16)
                        .active(true)
                        .build()
        );

        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(profile1.getId(), profile2.getId(), profile3.getId())
        );

        // When
        List<InvitationResponse> responses = invitationService.createStudentInvitations(teacher.getId(), request);

        // Then
        assertThat(responses).hasSize(3);
        assertThat(responses).allMatch(r -> r.inviteeRole() == InvitationRole.STUDENT);
        assertThat(responses).allMatch(r -> r.status() == InvitationStatus.PENDING);
        assertThat(responses).allMatch(r -> r.code() != null);

        // 각 프로필에 대해 초대가 생성되었는지 확인
        assertThat(invitationRepository.existsByStudentProfileIdAndStatusIn(
                profile1.getId(), PENDING_STATUSES)).isTrue();
        assertThat(invitationRepository.existsByStudentProfileIdAndStatusIn(
                profile2.getId(), PENDING_STATUSES)).isTrue();
        assertThat(invitationRepository.existsByStudentProfileIdAndStatusIn(
                profile3.getId(), PENDING_STATUSES)).isTrue();
    }

    @Test
    @DisplayName("Assistant는 자신에게 할당된 학생 프로필에 대해 일괄 초대를 생성할 수 있다")
    void createStudentInvitations_assistant_success() {
        // Given: Assistant에게 할당된 StudentProfile 2개
        StudentProfile profile1 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("학생A")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );
        StudentProfile profile2 = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("학생B")
                        .phoneNumber("010-2222-2222")
                        .parentPhone("010-8888-8888")
                        .schoolName("서울고")
                        .grade("고2")
                        .age(17)
                        .active(true)
                        .build()
        );

        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(profile1.getId(), profile2.getId())
        );

        // When
        List<InvitationResponse> responses = invitationService.createStudentInvitations(assistant.getId(), request);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.inviteeRole() == InvitationRole.STUDENT);
    }

    @Test
    @DisplayName("Assistant는 다른 Assistant에게 할당된 학생 프로필에 대해 초대를 생성할 수 없다")
    void createStudentInvitations_assistant_forbidden() {
        // Given: 다른 Assistant에게 할당된 프로필
        UUID otherAssistantId = UUID.randomUUID();
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(otherAssistantId)
                        .name("학생C")
                        .phoneNumber("010-3333-3333")
                        .parentPhone("010-7777-7777")
                        .schoolName("서울고")
                        .grade("고1")
                        .age(16)
                        .active(true)
                        .build()
        );

        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(profile.getId())
        );

        // When & Then
        assertThatThrownBy(() -> invitationService.createStudentInvitations(assistant.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    @DisplayName("이미 PENDING 초대가 있는 StudentProfile에 대해 중복 초대를 생성할 수 없다")
    void createStudentInvitations_duplicate_pending() {
        // Given: 이미 PENDING 초대가 있는 프로필
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("김철수")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );

        invitationRepository.save(
                Invitation.builder()
                        .senderId(teacher.getId())
                        .studentProfileId(profile.getId())
                        .inviteeRole(InvitationRole.STUDENT)
                        .status(InvitationStatus.PENDING)
                        .code(UUID.randomUUID().toString())
                        .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                        .build()
        );

        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(profile.getId())
        );

        // When & Then
        assertThatThrownBy(() -> invitationService.createStudentInvitations(teacher.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.INVITATION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("존재하지 않는 StudentProfile ID로 초대를 생성할 수 없다")
    void createStudentInvitations_profileNotFound() {
        // Given: 존재하지 않는 UUID
        UUID nonExistentId = UUID.randomUUID();
        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(nonExistentId)
        );

        // When & Then
        assertThatThrownBy(() -> invitationService.createStudentInvitations(teacher.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.NOT_FOUND);
    }

    @Test
    @DisplayName("memberId가 있는 StudentProfile(이미 계정 연동됨)에 대해 초대를 생성할 수 없다")
    void createStudentInvitations_alreadyLinked() {
        // Given: memberId가 있는 프로필 (이미 회원가입 완료)
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .memberId(UUID.randomUUID()) // 이미 계정 연동됨
                        .name("가입완료학생")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(true)
                        .build()
        );

        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(profile.getId())
        );

        // When & Then
        assertThatThrownBy(() -> invitationService.createStudentInvitations(teacher.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.INVALID_STUDENT_PROFILE);
    }

    @Test
    @DisplayName("active=false인 StudentProfile에 대해 초대를 생성할 수 없다")
    void createStudentInvitations_inactive() {
        // Given: active=false인 프로필
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .courseId(courseId)
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("비활성학생")
                        .phoneNumber("010-1111-1111")
                        .parentPhone("010-9999-9999")
                        .schoolName("서울고")
                        .grade("고3")
                        .age(18)
                        .active(false)
                        .build()
        );

        StudentInvitationCreateRequest request = new StudentInvitationCreateRequest(
                List.of(profile.getId())
        );

        // When & Then
        assertThatThrownBy(() -> invitationService.createStudentInvitations(teacher.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.INVALID_STUDENT_PROFILE);
    }
}
