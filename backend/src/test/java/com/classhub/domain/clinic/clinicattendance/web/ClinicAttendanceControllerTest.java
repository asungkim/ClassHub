package com.classhub.domain.clinic.clinicattendance.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.clinic.clinicattendance.application.ClinicAttendanceService;
import com.classhub.domain.clinic.clinicattendance.dto.request.ClinicAttendanceCreateRequest;
import com.classhub.domain.clinic.clinicattendance.dto.request.ClinicAttendanceMoveRequest;
import com.classhub.domain.clinic.clinicattendance.dto.response.ClinicAttendanceDetailResponse;
import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicattendance.dto.response.StudentClinicAttendanceListResponse;
import com.classhub.domain.clinic.clinicattendance.dto.response.StudentClinicAttendanceResponse;
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
class ClinicAttendanceControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClinicAttendanceService clinicAttendanceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getAttendances_shouldReturnList() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        ClinicAttendanceDetailResponse response = new ClinicAttendanceDetailResponse(
                UUID.randomUUID(),
                recordId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Student",
                "01012345678",
                "School",
                "MIDDLE_1",
                "01099998888",
                16
        );
        given(clinicAttendanceService.getAttendanceDetails(any(MemberPrincipal.class), eq(sessionId)))
                .willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/clinic-attendances")
                        .param("clinicSessionId", sessionId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].attendanceId").value(response.attendanceId().toString()))
                .andExpect(jsonPath("$.data[0].recordId").value(recordId.toString()))
                .andExpect(jsonPath("$.data[0].studentName").value("Student"))
                .andExpect(jsonPath("$.data[0].age").value(16));

        verify(clinicAttendanceService).getAttendanceDetails(any(MemberPrincipal.class), eq(sessionId));
    }

    @Test
    void addAttendance_shouldReturnCreated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ClinicAttendance attendance = createAttendance(sessionId);
        ClinicAttendanceCreateRequest request = new ClinicAttendanceCreateRequest(attendance.getStudentCourseRecordId());
        given(clinicAttendanceService.addAttendance(any(MemberPrincipal.class), eq(sessionId), eq(request.studentCourseRecordId())))
                .willReturn(attendance);

        mockMvc.perform(post("/api/v1/clinic-sessions/{sessionId}/attendances", sessionId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.attendanceId").value(attendance.getId().toString()));

        verify(clinicAttendanceService)
                .addAttendance(any(MemberPrincipal.class), eq(sessionId), eq(request.studentCourseRecordId()));
    }

    @Test
    void deleteAttendance_shouldReturnSuccess() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/clinic-attendances/{attendanceId}", attendanceId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(clinicAttendanceService).deleteAttendance(any(MemberPrincipal.class), eq(attendanceId));
    }

    @Test
    void requestAttendance_shouldReturnCreated() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        ClinicAttendance attendance = createAttendance(sessionId, recordId);
        given(clinicAttendanceService.requestAttendance(any(MemberPrincipal.class), eq(sessionId), eq(recordId)))
                .willReturn(attendance);

        mockMvc.perform(post("/api/v1/students/me/clinic-attendances")
                        .param("clinicSessionId", sessionId.toString())
                        .param("studentCourseRecordId", recordId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(studentId, MemberRole.STUDENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.attendanceId").value(attendance.getId().toString()));

        verify(clinicAttendanceService)
                .requestAttendance(any(MemberPrincipal.class), eq(sessionId), eq(recordId));
    }

    @Test
    void getStudentAttendances_shouldReturnList() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ClinicAttendance attendance = createAttendance(sessionId);
        StudentClinicAttendanceResponse item = new StudentClinicAttendanceResponse(
                attendance.getId(),
                sessionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2024, 3, 4),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                ClinicSessionType.REGULAR,
                false
        );
        given(clinicAttendanceService.getStudentAttendanceResponses(any(MemberPrincipal.class), any(), any()))
                .willReturn(new StudentClinicAttendanceListResponse(List.of(item)));

        mockMvc.perform(get("/api/v1/students/me/clinic-attendances")
                        .param("dateRange", "2024-03-01,2024-03-31")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(studentId, MemberRole.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.items[0].clinicSessionId").value(sessionId.toString()));

        verify(clinicAttendanceService).getStudentAttendanceResponses(any(MemberPrincipal.class), any(), any());
    }

    @Test
    void moveAttendance_shouldReturnSuccess() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID fromSessionId = UUID.randomUUID();
        UUID toSessionId = UUID.randomUUID();
        ClinicAttendance attendance = createAttendance(toSessionId);
        ClinicAttendanceMoveRequest request = new ClinicAttendanceMoveRequest(fromSessionId, toSessionId);
        given(clinicAttendanceService.moveAttendance(any(MemberPrincipal.class), eq(fromSessionId), eq(toSessionId)))
                .willReturn(attendance);

        mockMvc.perform(patch("/api/v1/students/me/clinic-attendances")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(studentId, MemberRole.STUDENT)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.attendanceId").value(attendance.getId().toString()));

        verify(clinicAttendanceService).moveAttendance(any(MemberPrincipal.class), eq(fromSessionId), eq(toSessionId));
    }

    private UsernamePasswordAuthenticationToken authToken(UUID memberId, MemberRole role) {
        MemberPrincipal principal = new MemberPrincipal(memberId, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(role.name()))
        );
    }

    private ClinicAttendance createAttendance(UUID sessionId) {
        return createAttendance(sessionId, UUID.randomUUID());
    }

    private ClinicAttendance createAttendance(UUID sessionId, UUID recordId) {
        ClinicAttendance attendance = ClinicAttendance.builder()
                .clinicSessionId(sessionId)
                .studentCourseRecordId(recordId)
                .build();
        ReflectionTestUtils.setField(attendance, "id", UUID.randomUUID());
        return attendance;
    }

}
