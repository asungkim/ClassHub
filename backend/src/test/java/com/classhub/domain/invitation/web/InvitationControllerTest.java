package com.classhub.domain.invitation.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.jwt.JwtProvider;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private Member teacher;
    private Member assistant;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
    @DisplayName("Assistant는 학생 초대를 생성할 수 있다")
    void createStudentInvitation_byAssistant() throws Exception {
        UUID profileId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new CreateStudentRequest("student@classhub.com", profileId));

        mockMvc.perform(post("/api/v1/invitations/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", bearer(assistant.getId(), MemberRole.ASSISTANT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteeRole").value("STUDENT"));
    }

    @Test
    @DisplayName("Teacher도 학생 초대를 생성할 수 있다")
    void createStudentInvitation_byTeacher() throws Exception {
        UUID profileId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new CreateStudentRequest("student2@classhub.com", profileId));

        mockMvc.perform(post("/api/v1/invitations/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", bearer(teacher.getId(), MemberRole.TEACHER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteeRole").value("STUDENT"));
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

    private String bearer(UUID memberId, MemberRole role) {
        return "Bearer " + jwtProvider.generateAccessToken(memberId, role.name());
    }

    private record CreateAssistantRequest(String targetEmail) {
    }

    private record CreateStudentRequest(String targetEmail, UUID studentProfileId) {
    }
}
