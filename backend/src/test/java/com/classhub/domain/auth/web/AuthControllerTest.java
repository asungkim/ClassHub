package com.classhub.domain.auth.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("유효한 입력이면 Teacher 계정을 생성하고 요약 정보를 반환한다")
    void registerTeacher_success() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        ));

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.memberId").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("teacher@classhub.com"))
                .andExpect(jsonPath("$.data.authority").value("TEACHER"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 DUPLICATE_EMAIL 코드로 실패한다")
    void registerTeacher_duplicateEmail() throws Exception {
        Member existing = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("선생님")
                .role(MemberRole.TEACHER)
                .build();
        memberRepository.save(existing);

        String payload = objectMapper.writeValueAsString(new RegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        ));

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("비밀번호 정책을 만족하지 못하면 400을 반환한다")
    void registerTeacher_invalidPasswordPolicy() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest(
                "teacher2@classhub.com",
                "simplepw",
                "김선생"
        ));

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private record RegisterRequest(String email, String password, String name) {
    }
}
