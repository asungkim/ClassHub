package com.classhub.domain.invitation.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.invitation.application.InvitationService;
import com.classhub.domain.invitation.dto.request.AssistantInvitationCreateRequest;
import com.classhub.domain.invitation.dto.response.InvitationResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class InvitationControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createAssistantInvitation_shouldReturnCreatedResponse() throws Exception {
        UUID teacherId = UUID.randomUUID();
        InvitationResponse response = new InvitationResponse(
                "INV-123",
                "assistant@classhub.com",
                LocalDateTime.now().plusDays(7)
        );
        given(invitationService.createAssistantInvitation(eq(teacherId), any(AssistantInvitationCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/invitations")
                        .with(csrf())
                        .with(authentication(teacherAuth(teacherId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssistantInvitationCreateRequest("assistant@classhub.com", null)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.code").value("INV-123"))
                .andExpect(jsonPath("$.data.targetEmail").value("assistant@classhub.com"));

        verify(invitationService).createAssistantInvitation(eq(teacherId), any(AssistantInvitationCreateRequest.class));
    }

    @Test
    void revokeInvitation_shouldReturnSuccess() throws Exception {
        UUID teacherId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/invitations/{code}/revoke", "INV-123")
                        .with(csrf())
                        .with(authentication(teacherAuth(teacherId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(invitationService).revokeInvitation(teacherId, "INV-123");
    }

    private UsernamePasswordAuthenticationToken teacherAuth(UUID id) {
        MemberPrincipal principal = new MemberPrincipal(id, MemberRole.TEACHER);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(MemberRole.TEACHER.name()))
        );
    }
}
