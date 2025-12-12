package com.classhub.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.auth.dto.request.InvitationRegisterRequest;
import com.classhub.domain.auth.dto.request.InvitationVerifyRequest;
import com.classhub.domain.auth.dto.response.InvitationVerifyResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
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
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member assistant;
    private Course course;

    @BeforeEach
    void setup() {
        invitationRepository.deleteAll();
        studentProfileRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Kim")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant Lee")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );

        course = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("Test Course")
                        .company("Test Company")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now())
                        .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                        .build()
        );
    }

    @Test
    @DisplayName("조교 초대 코드 검증 시 inviter와 role 정보를 반환한다")
    void verifyInvitation_assistant_success() {
        Invitation invitation = createAssistantInvitation(teacher.getId());

        InvitationVerifyResponse response = invitationAuthService.verify(
                new InvitationVerifyRequest(invitation.getCode())
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.ASSISTANT.name());
        assertThat(response.inviterId()).isEqualTo(teacher.getId());
        assertThat(response.inviterName()).isEqualTo("Teacher Kim");
        assertThat(response.studentProfile()).isNull();
    }

    @Test
    @DisplayName("학생 초대 코드 검증 시 studentProfile 정보를 포함한다")
    void verifyInvitation_student_success() {
        StudentProfile profile = createStudentProfile(course.getId(), teacher.getId(), assistant.getId());
        Invitation invitation = createStudentInvitation(assistant.getId(), profile.getId());

        InvitationVerifyResponse response = invitationAuthService.verify(
                new InvitationVerifyRequest(invitation.getCode())
        );

        assertThat(response.inviteeRole()).isEqualTo(InvitationRole.STUDENT.name());
        assertThat(response.inviterId()).isEqualTo(assistant.getId());
        assertThat(response.inviterName()).isEqualTo("Assistant Lee");
        assertThat(response.studentProfile()).isNotNull();
        assertThat(response.studentProfile().id()).isEqualTo(profile.getId());
        assertThat(response.studentProfile().name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("만료된 초대 코드는 INVALID_INVITATION 예외를 던진다")
    void verifyInvitation_expired() {
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .senderId(teacher.getId())
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
    @DisplayName("조교 초대로 회원가입하면 Assistant 계정을 생성하고 무제한 링크는 PENDING 유지")
    void registerInvited_assistant_unlimitedLink() {
        Invitation invitation = createAssistantInvitation(teacher.getId());

        var response = invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "newassistant@classhub.com", "Classhub!1", "조교")
        );

        Member saved = memberRepository.findByEmail("newassistant@classhub.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.ASSISTANT);
        assertThat(saved.getTeacherId()).isEqualTo(teacher.getId());
        assertThat(response.accessToken()).isNotBlank();

        Invitation updatedInvitation = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertThat(updatedInvitation.getUseCount()).isEqualTo(1);
        assertThat(updatedInvitation.getStatus()).isEqualTo(InvitationStatus.PENDING); // 무제한 링크는 PENDING 유지
    }

    @Test
    @DisplayName("학생 초대로 회원가입하면 Student 계정을 생성하고 StudentProfile에 memberId 연결")
    void registerInvited_student_success() {
        StudentProfile profile = createStudentProfile(course.getId(), teacher.getId(), assistant.getId());
        Invitation invitation = createStudentInvitation(assistant.getId(), profile.getId());

        var response = invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "student@classhub.com", "Classhub!1", "홍길동")
        );

        Member saved = memberRepository.findByEmail("student@classhub.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.STUDENT);
        assertThat(saved.getTeacherId()).isEqualTo(teacher.getId());
        assertThat(response.accessToken()).isNotBlank();

        StudentProfile updatedProfile = studentProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updatedProfile.getMemberId()).isEqualTo(saved.getId());

        Invitation updatedInvitation = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertThat(updatedInvitation.getUseCount()).isEqualTo(1);
        assertThat(updatedInvitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED); // 학생 초대는 단일 사용이므로 ACCEPTED
    }

    @Test
    @DisplayName("중복 이메일로 회원가입을 시도하면 DUPLICATE_EMAIL 예외")
    void registerInvited_duplicateEmail() {
        Invitation invitation = createAssistantInvitation(teacher.getId());

        assertThatThrownBy(() -> invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "teacher@classhub.com", "Classhub!1", "중복")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("잘못된 초대 코드로 회원가입을 시도하면 INVALID_INVITATION")
    void registerInvited_invalidCode() {
        assertThatThrownBy(() -> invitationAuthService.registerInvited(
                new InvitationRegisterRequest("invalid", "user@classhub.com", "Classhub!1", "사용자")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.INVALID_INVITATION.getMessage());
    }

    @Test
    @DisplayName("이미 사용된 학생 초대 코드는 재사용 불가")
    void registerInvited_student_alreadyUsed() {
        StudentProfile profile = createStudentProfile(course.getId(), teacher.getId(), assistant.getId());
        Invitation invitation = createStudentInvitation(assistant.getId(), profile.getId());

        // 첫 번째 회원가입 성공
        invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "student1@classhub.com", "Classhub!1", "학생1")
        );

        // 같은 코드로 두 번째 회원가입 시도 (maxUses=1이므로 실패해야 함)
        assertThatThrownBy(() -> invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "student2@classhub.com", "Classhub!1", "학생2")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.INVALID_INVITATION.getMessage());
    }

    @Test
    @DisplayName("조교 초대 링크는 여러 명이 사용 가능")
    void registerInvited_assistant_multipleUse() {
        Invitation invitation = createAssistantInvitation(teacher.getId());

        // 첫 번째 조교 등록
        invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "assistant1@classhub.com", "Classhub!1", "조교1")
        );

        // 같은 코드로 두 번째 조교 등록 (maxUses=-1 무제한이므로 성공)
        var response = invitationAuthService.registerInvited(
                new InvitationRegisterRequest(invitation.getCode(), "assistant2@classhub.com", "Classhub!1", "조교2")
        );

        assertThat(response.accessToken()).isNotBlank();
        Member saved = memberRepository.findByEmail("assistant2@classhub.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.ASSISTANT);

        Invitation updatedInvitation = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertThat(updatedInvitation.getUseCount()).isEqualTo(2);
        assertThat(updatedInvitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    private Invitation createAssistantInvitation(UUID senderId) {
        return invitationRepository.save(Invitation.builder()
                .senderId(senderId)
                .inviteeRole(InvitationRole.ASSISTANT)
                .code(UUID.randomUUID().toString())
                .maxUses(-1) // 무제한
                .useCount(0)
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusYears(10))
                .status(InvitationStatus.PENDING)
                .build());
    }

    private Invitation createStudentInvitation(UUID senderId, UUID studentProfileId) {
        return invitationRepository.save(Invitation.builder()
                .senderId(senderId)
                .inviteeRole(InvitationRole.STUDENT)
                .studentProfileId(studentProfileId)
                .code(UUID.randomUUID().toString())
                .maxUses(1) // 단일 사용
                .useCount(0)
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .status(InvitationStatus.PENDING)
                .build());
    }

    private StudentProfile createStudentProfile(UUID courseId, UUID teacherId, UUID assistantId) {
        return studentProfileRepository.save(StudentProfile.builder()
                .courseId(courseId)
                .teacherId(teacherId)
                .assistantId(assistantId)
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .parentPhone("010-9876-5432")
                .schoolName("서울고등학교")
                .grade("고1")
                .age(16)
                .active(true)
                .build());
    }
}
