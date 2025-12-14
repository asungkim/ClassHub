package com.classhub.domain.invitation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.jwt.JwtProvider;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "JWT_SECRET_KEY=0123456789012345678901234567890123456789012345678901234567890123")
@ActiveProfiles("test")
class InvitationControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private StudentCourseEnrollmentRepository studentCourseEnrollmentRepository;

    @Autowired
    private PersonalLessonRepository personalLessonRepository;

    private Member teacher;
    private Member assistant;
    private UUID courseId;


    @BeforeEach
    void setUp() {
        personalLessonRepository.deleteAll();
        studentCourseEnrollmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        courseId = UUID.randomUUID();

        teacher = memberRepository.save(Member.builder()
                .email("teacher@classhub.com")
                .password(passwordEncoder.encode("Classhub!1"))
                .name("Teacher")
                .role(MemberRole.TEACHER)
                .build());
        assistant = memberRepository.save(Member.builder()
                .email("assistant@classhub.com")
                .password(passwordEncoder.encode("Classhub!1"))
                .name("Assistant")
                .role(MemberRole.ASSISTANT)
                .teacherId(teacher.getId())
                .build());
    }

    @Test
    @DisplayName("Teacher는 조교 초대를 생성할 수 있다")
    void createAssistantInvitation() throws Exception {
        String payload = objectMapper.writeValueAsString(new CreateAssistantRequest("newassistant@classhub.com"));

        mockMvc.perform(post("/api/v1/invitations/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteeRole").value("ASSISTANT"));
    }

    @Test
    @DisplayName("Assistant는 학생 일괄 초대를 생성할 수 있다")
    void createStudentInvitation_byAssistant() throws Exception {
        // Given: Assistant에게 할당된 StudentProfile 생성
        StudentProfile profile1 = studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("학생A")
                .phoneNumber("010-1111-1111")
                .parentPhone("010-9999-9999")
                .schoolName("서울고")
                .grade("고3")
                .age(18)
                .active(true)
                .build());
        StudentProfile profile2 = studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("학생B")
                .phoneNumber("010-2222-2222")
                .parentPhone("010-8888-8888")
                .schoolName("서울고")
                .grade("고2")
                .age(17)
                .active(true)
                .build());

        String payload = objectMapper.writeValueAsString(
                new CreateStudentRequest(java.util.List.of(profile1.getId(), profile2.getId()))
        );

        mockMvc.perform(post("/api/v1/invitations/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", bearer(assistant.getId(), MemberRole.ASSISTANT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].inviteeRole").value("STUDENT"))
                .andExpect(jsonPath("$.data[1].inviteeRole").value("STUDENT"));
    }

    @Test
    @DisplayName("Teacher도 학생 일괄 초대를 생성할 수 있다")
    void createStudentInvitation_byTeacher() throws Exception {
        // Given: Teacher의 StudentProfile 생성
        StudentProfile profile = studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("학생C")
                .phoneNumber("010-3333-3333")
                .parentPhone("010-7777-7777")
                .schoolName("서울고")
                .grade("고1")
                .age(16)
                .active(true)
                .build());

        String payload = objectMapper.writeValueAsString(
                new CreateStudentRequest(java.util.List.of(profile.getId()))
        );

        mockMvc.perform(post("/api/v1/invitations/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].inviteeRole").value("STUDENT"));
    }

    @Test
    @DisplayName("초대 목록을 조회할 수 있다")
    void listInvitations() throws Exception {
        invitationRepository.save(Invitation.builder()
                .senderId(teacher.getId())
                .targetEmail("pending@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code(UUID.randomUUID().toString())
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build());

        mockMvc.perform(get("/api/v1/invitations/assistant")
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("초대를 취소할 수 있다")
    void revokeInvitation() throws Exception {
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .senderId(teacher.getId())
                .targetEmail("cancel@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .code("cancel-code")
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build());

        mockMvc.perform(delete("/api/v1/invitations/{code}", invitation.getCode())
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Teacher는 학생 초대 후보를 조회할 수 있다")
    void findStudentCandidates_teacher() throws Exception {
        // Given: 초대되지 않은 StudentProfile 2개
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("김철수")
                .phoneNumber("010-1111-1111")
                .parentPhone("010-9999-9999")
                .schoolName("서울고")
                .grade("고3")
                .age(18)
                .active(true)
                .build());
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("이영희")
                .phoneNumber("010-2222-2222")
                .parentPhone("010-8888-8888")
                .schoolName("서울고")
                .grade("고2")
                .age(17)
                .active(true)
                .build());

        // When & Then
        mockMvc.perform(get("/api/v1/invitations/student/candidates")
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].phoneNumber").exists());
    }

    @Test
    @DisplayName("Assistant는 자신에게 할당된 학생 초대 후보를 조회할 수 있다")
    void findStudentCandidates_assistant() throws Exception {
        // Given: Assistant에게 할당된 프로필 1개
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("학생A")
                .phoneNumber("010-1111-1111")
                .parentPhone("010-9999-9999")
                .schoolName("서울고")
                .grade("고3")
                .age(18)
                .active(true)
                .build());
        // 다른 조교에게 할당된 프로필 (조회되지 않아야 함)
        UUID otherAssistantId = UUID.randomUUID();
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(otherAssistantId)
                .name("학생B")
                .phoneNumber("010-2222-2222")
                .parentPhone("010-8888-8888")
                .schoolName("서울고")
                .grade("고2")
                .age(17)
                .active(true)
                .build());

        // When & Then
        mockMvc.perform(get("/api/v1/invitations/student/candidates")
                        .header("Authorization", bearer(assistant.getId(), MemberRole.ASSISTANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("학생A"));
    }

    @Test
    @DisplayName("학생 초대 후보 조회 시 이름으로 필터링할 수 있다")
    void findStudentCandidates_withNameFilter() throws Exception {
        // Given
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("김철수")
                .phoneNumber("010-1111-1111")
                .parentPhone("010-9999-9999")
                .schoolName("서울고")
                .grade("고3")
                .age(18)
                .active(true)
                .build());
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("이영희")
                .phoneNumber("010-2222-2222")
                .parentPhone("010-8888-8888")
                .schoolName("서울고")
                .grade("고2")
                .age(17)
                .active(true)
                .build());

        // When & Then: "철수" 검색
        mockMvc.perform(get("/api/v1/invitations/student/candidates")
                        .param("name", "철수")
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("김철수"));
    }

    @Test
    @DisplayName("PENDING 초대가 있는 학생은 후보에서 제외된다")
    void findStudentCandidates_excludePending() throws Exception {
        // Given: 초대된 프로필과 초대되지 않은 프로필
        StudentProfile invitedProfile = studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("초대됨")
                .phoneNumber("010-1111-1111")
                .parentPhone("010-9999-9999")
                .schoolName("서울고")
                .grade("고3")
                .age(18)
                .active(true)
                .build());
        studentProfileRepository.save(StudentProfile.builder()
                .teacherId(teacher.getId())
                .assistantId(assistant.getId())
                .name("초대안됨")
                .phoneNumber("010-2222-2222")
                .parentPhone("010-8888-8888")
                .schoolName("서울고")
                .grade("고2")
                .age(17)
                .active(true)
                .build());

        // PENDING 초대 생성
        invitationRepository.save(Invitation.builder()
                .senderId(teacher.getId())
                .studentProfileId(invitedProfile.getId())
                .inviteeRole(InvitationRole.STUDENT)
                .status(InvitationStatus.PENDING)
                .code(UUID.randomUUID().toString())
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build());

        // When & Then: "초대안됨"만 반환
        mockMvc.perform(get("/api/v1/invitations/student/candidates")
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("초대안됨"));
    }

    private String bearer(UUID memberId, MemberRole role) {
        return "Bearer " + jwtProvider.generateAccessToken(memberId, role.name());
    }

    @Test
    @DisplayName("Teacher는 조교 초대 링크를 생성할 수 있다")
    void createAssistantLink_success() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/assistant/link")
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteeRole").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.code").exists());
    }

    @Test
    @DisplayName("Teacher가 새 조교 초대 링크를 생성하면 기존 PENDING 초대가 REVOKED된다")
    void createAssistantLink_autoRevoke() throws Exception {
        // Given: 기존 PENDING 조교 초대 생성
        Invitation existing = invitationRepository.save(Invitation.builder()
                .senderId(teacher.getId())
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .code("existing-code")
                .maxUses(-1)
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusYears(10))
                .build());

        // When: 새 조교 초대 링크 생성
        mockMvc.perform(post("/api/v1/invitations/assistant/link")
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isCreated());

        // Then: 기존 초대가 REVOKED로 전환
        Invitation refreshed = invitationRepository.findById(existing.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    @DisplayName("Assistant는 조교 초대 링크를 생성할 수 없다")
    void createAssistantLink_assistantForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/assistant/link")
                        .header("Authorization", bearer(assistant.getId(), MemberRole.ASSISTANT)))
                .andExpect(status().isForbidden());
    }

    private record CreateAssistantRequest(String targetEmail) {
    }

    private record CreateStudentRequest(List<UUID> studentProfileIds) {
    }
}
