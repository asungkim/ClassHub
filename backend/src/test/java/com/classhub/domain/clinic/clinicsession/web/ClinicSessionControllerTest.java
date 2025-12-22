package com.classhub.domain.clinic.clinicsession.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.clinic.clinicsession.application.ClinicSessionService;
import com.classhub.domain.clinic.clinicsession.dto.request.ClinicSessionEmergencyCreateRequest;
import com.classhub.domain.clinic.clinicsession.dto.request.ClinicSessionRegularCreateRequest;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ClinicSessionControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClinicSessionService clinicSessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getSessions_shouldReturnTeacherSessions() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSession session = createSession(UUID.randomUUID(), teacherId, branchId, ClinicSessionType.REGULAR);
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 7);
        given(clinicSessionService.getSessions(any(MemberPrincipal.class), isNull(), eq(branchId), eq(start), eq(end)))
                .willReturn(List.of(session));

        mockMvc.perform(get("/api/v1/clinic-sessions")
                        .param("dateRange", "2024-03-01,2024-03-07")
                        .param("branchId", branchId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].sessionId").value(session.getId().toString()));

        verify(clinicSessionService).getSessions(any(MemberPrincipal.class), isNull(), eq(branchId), eq(start), eq(end));
    }

    @Test
    void getSessions_shouldReturnAssistantSessions() throws Exception {
        UUID assistantId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSession session = createSession(UUID.randomUUID(), teacherId, branchId, ClinicSessionType.EMERGENCY);
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 7);
        given(clinicSessionService.getSessions(any(MemberPrincipal.class), eq(teacherId), eq(branchId), eq(start), eq(end)))
                .willReturn(List.of(session));

        mockMvc.perform(get("/api/v1/clinic-sessions")
                        .param("dateRange", "2024-03-01,2024-03-07")
                        .param("branchId", branchId.toString())
                        .param("teacherId", teacherId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(assistantId, MemberRole.ASSISTANT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].sessionId").value(session.getId().toString()));

        verify(clinicSessionService).getSessions(any(MemberPrincipal.class), eq(teacherId), eq(branchId), eq(start), eq(end));
    }

    @Test
    void getSessions_shouldReturnStudentSessions() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSession session = createSession(UUID.randomUUID(), teacherId, branchId, ClinicSessionType.REGULAR);
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 7);
        given(clinicSessionService.getSessions(any(MemberPrincipal.class), eq(teacherId), eq(branchId), eq(start), eq(end)))
                .willReturn(List.of(session));

        mockMvc.perform(get("/api/v1/clinic-sessions")
                        .param("dateRange", "2024-03-01,2024-03-07")
                        .param("branchId", branchId.toString())
                        .param("teacherId", teacherId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(studentId, MemberRole.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].sessionId").value(session.getId().toString()));

        verify(clinicSessionService).getSessions(any(MemberPrincipal.class), eq(teacherId), eq(branchId), eq(start), eq(end));
    }

    @Test
    void createRegularSession_shouldReturnCreated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSession session = createSession(UUID.randomUUID(), teacherId, UUID.randomUUID(), ClinicSessionType.REGULAR);
        given(clinicSessionService.createRegularSession(eq(teacherId), eq(slotId), any(LocalDate.class)))
                .willReturn(session);
        ClinicSessionRegularCreateRequest request = new ClinicSessionRegularCreateRequest(LocalDate.of(2024, 3, 4));

        mockMvc.perform(post("/api/v1/clinic-slots/{slotId}/sessions", slotId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.sessionId").value(session.getId().toString()));

        verify(clinicSessionService).createRegularSession(eq(teacherId), eq(slotId), eq(request.date()));
    }

    @Test
    void createEmergencySession_shouldReturnCreated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSession session = createSession(UUID.randomUUID(), teacherId, branchId, ClinicSessionType.EMERGENCY);
        ClinicSessionEmergencyCreateRequest request = new ClinicSessionEmergencyCreateRequest(
                branchId,
                null,
                LocalDate.of(2024, 3, 5),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                6
        );
        given(clinicSessionService.createEmergencySession(any(MemberPrincipal.class), any(ClinicSessionEmergencyCreateRequest.class)))
                .willReturn(session);

        mockMvc.perform(post("/api/v1/clinic-sessions/emergency")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.sessionId").value(session.getId().toString()));

        verify(clinicSessionService).createEmergencySession(any(MemberPrincipal.class), any(ClinicSessionEmergencyCreateRequest.class));
    }

    @Test
    void cancelSession_shouldReturnSuccess() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/clinic-sessions/{sessionId}/cancel", sessionId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(clinicSessionService).cancelSession(any(MemberPrincipal.class), eq(sessionId));
    }

    private UsernamePasswordAuthenticationToken authToken(UUID memberId, MemberRole role) {
        MemberPrincipal principal = new MemberPrincipal(memberId, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(role.name()))
        );
    }

    private ClinicSession createSession(UUID sessionId,
                                        UUID teacherId,
                                        UUID branchId,
                                        ClinicSessionType type) {
        ClinicSession session = ClinicSession.builder()
                .slotId(type == ClinicSessionType.REGULAR ? UUID.randomUUID() : null)
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(type)
                .creatorMemberId(type == ClinicSessionType.EMERGENCY ? teacherId : null)
                .date(LocalDate.of(2024, 3, 4))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .capacity(10)
                .canceled(false)
                .build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        return session;
    }
}
