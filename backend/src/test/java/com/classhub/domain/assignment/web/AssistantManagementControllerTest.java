package com.classhub.domain.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.assignment.application.AssistantManagementService;
import com.classhub.domain.assignment.dto.AssistantAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.response.AssistantAssignmentResponse;
import com.classhub.domain.assignment.dto.response.AssistantAssignmentResponse.AssistantProfile;
import com.classhub.domain.assignment.dto.response.AssistantSearchResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AssistantManagementControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssistantManagementService assistantManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getAssistants_shouldReturnPagedAssignments() throws Exception {
        UUID assistantId = UUID.randomUUID();
        PageResponse<AssistantAssignmentResponse> pageResponse = new PageResponse<>(
                List.of(new AssistantAssignmentResponse(
                        UUID.randomUUID(),
                        new AssistantProfile(assistantId, "Assistant Kim", "assistant@classhub.com", "01011112222"),
                        true,
                        LocalDateTime.now().minusDays(1),
                        null
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        org.mockito.BDDMockito.given(assistantManagementService.getAssistantAssignments(
                eq(teacherId),
                eq(AssistantAssignmentStatusFilter.ACTIVE),
                eq(0),
                eq(20)
        )).willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/teachers/me/assistants")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].assistant.name").value("Assistant Kim"));

        verify(assistantManagementService).getAssistantAssignments(
                eq(teacherId),
                eq(AssistantAssignmentStatusFilter.ACTIVE),
                eq(0),
                eq(20)
        );
    }

    @Test
    void updateAssistantStatus_shouldInvokeService() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        AssistantAssignmentResponse response = new AssistantAssignmentResponse(
                assignmentId,
                new AssistantProfile(UUID.randomUUID(), "Assistant", "assistant@classhub.com", "01022223333"),
                false,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now()
        );
        org.mockito.BDDMockito.given(assistantManagementService.updateAssistantStatus(
                eq(teacherId),
                eq(assignmentId),
                eq(false)
        )).willReturn(response);

        mockMvc.perform(patch("/api/v1/teachers/me/assistants/{assignmentId}", assignmentId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ToggleRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId.toString()));

        verify(assistantManagementService).updateAssistantStatus(teacherId, assignmentId, false);
    }

    @Test
    void searchAssistants_shouldReturnResults() throws Exception {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        UUID assistantId = UUID.randomUUID();
        AssistantSearchResponse response = new AssistantSearchResponse(
                assistantId,
                "Assistant Park",
                "assistant@classhub.com",
                "01044445555",
                AssistantSearchResponse.AssignmentStatus.NOT_ASSIGNED,
                null,
                null,
                null
        );
        org.mockito.BDDMockito.given(assistantManagementService.searchAssistants(eq(teacherId), eq("assistant")))
                .willReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/v1/teachers/me/assistants/search")
                        .param("name", "assistant")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].email").value("assistant@classhub.com"));

        verify(assistantManagementService).searchAssistants(eq(teacherId), eq("assistant"));
    }

    @Test
    void assignAssistant_shouldReturnCreatedAssignment() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID assistantId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        AssistantAssignmentResponse response = new AssistantAssignmentResponse(
                UUID.randomUUID(),
                new AssistantProfile(assistantId, "Assistant", "assistant@classhub.com", "01055556666"),
                true,
                LocalDateTime.now(),
                null
        );
        org.mockito.BDDMockito.given(assistantManagementService.assignAssistant(eq(teacherId), eq(assistantId)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/teachers/me/assistants")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignRequest(assistantId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.assistant.memberId").value(assistantId.toString()));

        verify(assistantManagementService).assignAssistant(teacherId, assistantId);
    }

    private record ToggleRequest(boolean enabled) {
    }

    private record AssignRequest(UUID assistantMemberId) {
    }
}
