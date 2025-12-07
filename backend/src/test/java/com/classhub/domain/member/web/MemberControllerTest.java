package com.classhub.domain.member.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class MemberControllerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private Member teacher;
    private Member assistantActive;
    private Member assistantInactive;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters((request, response, chain) -> {
                    Object contextAttr = request.getAttribute(
                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
                    );
                    if (contextAttr instanceof SecurityContext securityContext) {
                        SecurityContextHolder.setContext(securityContext);
                    }
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
                .build();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        assistantActive = memberRepository.save(
                Member.builder()
                        .email("assistant.active@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Active Assistant")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );

        assistantInactive = memberRepository.save(
                Member.builder()
                        .email("assistant.inactive@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Inactive Assistant")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );
        assistantInactive.deactivate();
        memberRepository.save(assistantInactive);
    }

    @Test
    @DisplayName("Teacher는 소속 조교 목록을 활성 상태 필터로 조회할 수 있다")
    void listAssistants_withActiveFilter() throws Exception {
        mockMvc.perform(get("/api/v1/members")
                        .with(teacherPrincipal())
                        .param("role", "ASSISTANT")
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("assistant.active@classhub.com"));
    }

    @Test
    @DisplayName("Teacher는 자신의 조교를 비활성화할 수 있다")
    void deactivateAssistant_success() throws Exception {
        mockMvc.perform(patch("/api/v1/members/{id}/deactivate", assistantActive.getId())
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Member updated = memberRepository.findById(assistantActive.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @DisplayName("Teacher는 비활성 조교를 다시 활성화할 수 있다")
    void activateAssistant_success() throws Exception {
        mockMvc.perform(patch("/api/v1/members/{id}/activate", assistantInactive.getId())
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Member updated = memberRepository.findById(assistantInactive.getId()).orElseThrow();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    @DisplayName("다른 Teacher 소속 조교는 비활성화할 수 없다")
    void deactivateAssistant_forbiddenForOtherTeacher() throws Exception {
        Member otherTeacher = memberRepository.save(
                Member.builder()
                        .email("other.teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Other Teacher")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        mockMvc.perform(patch("/api/v1/members/{id}/deactivate", assistantActive.getId())
                        .with(principal(otherTeacher.getId()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private RequestPostProcessor teacherPrincipal() {
        return principal(teacher.getId());
    }

    private RequestPostProcessor principal(UUID memberId) {
        return request -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new TestingAuthenticationToken(
                    new MemberPrincipal(memberId),
                    null,
                    List.of(new SimpleGrantedAuthority("TEACHER"))
            ));
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );
            return request;
        };
    }
}
