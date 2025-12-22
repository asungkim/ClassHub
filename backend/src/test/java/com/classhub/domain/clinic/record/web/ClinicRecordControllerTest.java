package com.classhub.domain.clinic.record.web;

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

import com.classhub.domain.clinic.record.application.ClinicRecordService;
import com.classhub.domain.clinic.record.dto.request.ClinicRecordCreateRequest;
import com.classhub.domain.clinic.record.dto.request.ClinicRecordUpdateRequest;
import com.classhub.domain.clinic.record.model.ClinicRecord;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
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
class ClinicRecordControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClinicRecordService clinicRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createRecord_shouldReturnCreated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        ClinicRecordCreateRequest request = new ClinicRecordCreateRequest(
                attendanceId,
                "title",
                "content",
                "done"
        );
        ClinicRecord record = createRecord(attendanceId, teacherId);
        given(clinicRecordService.createRecord(any(MemberPrincipal.class), any(ClinicRecordCreateRequest.class)))
                .willReturn(record);

        mockMvc.perform(post("/api/v1/clinic-records")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.recordId").value(record.getId().toString()));

        verify(clinicRecordService).createRecord(any(MemberPrincipal.class), any(ClinicRecordCreateRequest.class));
    }

    @Test
    void getRecord_shouldReturnRecord() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        ClinicRecord record = createRecord(attendanceId, teacherId);
        given(clinicRecordService.getRecord(any(MemberPrincipal.class), eq(attendanceId)))
                .willReturn(record);

        mockMvc.perform(get("/api/v1/clinic-records")
                        .param("clinicAttendanceId", attendanceId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.recordId").value(record.getId().toString()));

        verify(clinicRecordService).getRecord(any(MemberPrincipal.class), eq(attendanceId));
    }

    @Test
    void updateRecord_shouldReturnUpdated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        ClinicRecordUpdateRequest request = new ClinicRecordUpdateRequest("new", "updated", "done");
        ClinicRecord record = createRecord(UUID.randomUUID(), teacherId);
        ReflectionTestUtils.setField(record, "id", recordId);
        given(clinicRecordService.updateRecord(any(MemberPrincipal.class), eq(recordId), any(ClinicRecordUpdateRequest.class)))
                .willReturn(record);

        mockMvc.perform(patch("/api/v1/clinic-records/{recordId}", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.recordId").value(recordId.toString()));

        verify(clinicRecordService)
                .updateRecord(any(MemberPrincipal.class), eq(recordId), any(ClinicRecordUpdateRequest.class));
    }

    @Test
    void deleteRecord_shouldReturnSuccess() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/clinic-records/{recordId}", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(clinicRecordService).deleteRecord(any(MemberPrincipal.class), eq(recordId));
    }

    private UsernamePasswordAuthenticationToken authToken(UUID memberId, MemberRole role) {
        MemberPrincipal principal = new MemberPrincipal(memberId, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                java.util.List.of(new SimpleGrantedAuthority(role.name()))
        );
    }

    private ClinicRecord createRecord(UUID attendanceId, UUID writerId) {
        ClinicRecord record = ClinicRecord.builder()
                .clinicAttendanceId(attendanceId)
                .writerId(writerId)
                .title("title")
                .content("content")
                .homeworkProgress("done")
                .build();
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        return record;
    }
}
