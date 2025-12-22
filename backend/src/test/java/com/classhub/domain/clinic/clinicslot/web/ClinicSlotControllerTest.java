package com.classhub.domain.clinic.clinicslot.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.classhub.domain.clinic.clinicslot.application.ClinicSlotService;
import com.classhub.domain.clinic.clinicslot.dto.request.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.clinicslot.dto.request.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
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
class ClinicSlotControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClinicSlotService clinicSlotService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createSlot_shouldReturnCreated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(UUID.randomUUID(), teacherId, branchId);
        given(clinicSlotService.createSlot(eq(teacherId), any(ClinicSlotCreateRequest.class)))
                .willReturn(slot);
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                10
        );

        mockMvc.perform(post("/api/v1/clinic-slots")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.slotId").value(slot.getId().toString()));

        verify(clinicSlotService).createSlot(eq(teacherId), any(ClinicSlotCreateRequest.class));
    }

    @Test
    void getSlots_shouldReturnTeacherSlots() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(UUID.randomUUID(), teacherId, branchId);
        given(clinicSlotService.getSlots(any(MemberPrincipal.class), eq(branchId), isNull(), isNull()))
                .willReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/clinic-slots")
                        .param("branchId", branchId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].slotId").value(slot.getId().toString()));

        verify(clinicSlotService).getSlots(any(MemberPrincipal.class), eq(branchId), isNull(), isNull());
    }

    @Test
    void getSlots_shouldReturnAssistantSlots() throws Exception {
        UUID assistantId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(UUID.randomUUID(), teacherId, branchId);
        given(clinicSlotService.getSlots(any(MemberPrincipal.class), eq(branchId), eq(teacherId), isNull()))
                .willReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/clinic-slots")
                        .param("branchId", branchId.toString())
                        .param("teacherId", teacherId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(assistantId, MemberRole.ASSISTANT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].slotId").value(slot.getId().toString()));

        verify(clinicSlotService).getSlots(any(MemberPrincipal.class), eq(branchId), eq(teacherId), isNull());
    }

    @Test
    void getSlots_shouldReturnStudentSlots() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        ClinicSlot slot = createSlot(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        given(clinicSlotService.getSlots(any(MemberPrincipal.class), isNull(), isNull(), eq(courseId)))
                .willReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/clinic-slots")
                        .param("courseId", courseId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(studentId, MemberRole.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].slotId").value(slot.getId().toString()));

        verify(clinicSlotService).getSlots(any(MemberPrincipal.class), isNull(), isNull(), eq(courseId));
    }

    @Test
    void updateSlot_shouldReturnUpdated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, UUID.randomUUID());
        ClinicSlotUpdateRequest request = new ClinicSlotUpdateRequest(
                DayOfWeek.TUESDAY,
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                8
        );
        given(clinicSlotService.updateSlot(eq(teacherId), eq(slotId), any(ClinicSlotUpdateRequest.class)))
                .willReturn(slot);

        mockMvc.perform(patch("/api/v1/clinic-slots/{slotId}", slotId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.slotId").value(slotId.toString()));

        verify(clinicSlotService).updateSlot(eq(teacherId), eq(slotId), any(ClinicSlotUpdateRequest.class));
    }

    @Test
    void deleteSlot_shouldReturnSuccess() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/clinic-slots/{slotId}", slotId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authToken(teacherId, MemberRole.TEACHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(clinicSlotService).deleteSlot(teacherId, slotId);
    }

    private UsernamePasswordAuthenticationToken authToken(UUID memberId, MemberRole role) {
        MemberPrincipal principal = new MemberPrincipal(memberId, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(role.name()))
        );
    }

    private ClinicSlot createSlot(UUID slotId, UUID teacherId, UUID branchId) {
        ClinicSlot slot = ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(10)
                .build();
        ReflectionTestUtils.setField(slot, "id", slotId);
        return slot;
    }
}
