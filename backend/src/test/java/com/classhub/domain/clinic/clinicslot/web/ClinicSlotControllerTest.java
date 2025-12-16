package com.classhub.domain.clinic.clinicslot.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.clinic.clinicslot.application.ClinicSlotService;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.clinicslot.dto.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import java.time.DayOfWeek;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ClinicSlotController API")
class ClinicSlotControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClinicSlotRepository clinicSlotRepository;

    @Autowired
    private ClinicSlotService clinicSlotService;

    @Autowired
    private MemberRepository memberRepository;

    private MockMvc mockMvc;

    private Member teacher;
    private Member assistant;

    @BeforeEach
    void setUp() {
        clinicSlotRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.dev")
                        .password("password")
                        .name("Teacher One")
                        .role(MemberRole.TEACHER)
                        .build()
        );
        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.dev")
                        .password("password")
                        .name("Assistant One")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot을 생성할 수 있다")
    void shouldCreateSlot_whenTeacherAuthenticated() throws Exception {
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10);

        mockMvc.perform(post("/api/v1/clinic-slots")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.capacity").value(10));
    }

    @Test
    @DisplayName("유효성 검증 실패 시 400 응답을 반환한다")
    void shouldReturn400_whenInvalidRequest() throws Exception {
        String invalidJson = """
                {
                    "dayOfWeek": "MONDAY",
                    "startTime": "14:00",
                    "endTime": "13:00",
                    "capacity": 0
                }
                """;

        mockMvc.perform(post("/api/v1/clinic-slots")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Teacher 권한이 아니면 ClinicSlot 생성이 거부된다")
    void shouldReturn403_whenNotTeacher() throws Exception {
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10);

        mockMvc.perform(post("/api/v1/clinic-slots")
                        .with(assistantPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Teacher는 파라미터로 ClinicSlot 목록을 조회할 수 있다")
    void shouldGetSlots_withFilters() throws Exception {
        clinicSlotService.createSlot(teacher.getId(), new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10));
        clinicSlotService.createSlot(teacher.getId(), new ClinicSlotCreateRequest(DayOfWeek.TUESDAY, "10:00", "12:00", 8));

        mockMvc.perform(get("/api/v1/clinic-slots")
                        .with(teacherPrincipal())
                        .param("isActive", "true")
                        .param("dayOfWeek", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot 상세를 조회할 수 있다")
    void shouldGetSlot_whenValidId() throws Exception {
        var response = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        mockMvc.perform(get("/api/v1/clinic-slots/{slotId}", response.id())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.id().toString()));
    }

    @Test
    @DisplayName("존재하지 않는 ClinicSlot 조회 시 404 응답을 반환한다")
    void shouldReturn404_whenSlotNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/clinic-slots/{slotId}", UUID.randomUUID())
                        .with(teacherPrincipal()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot을 수정할 수 있다")
    void shouldUpdateSlot_whenValidData() throws Exception {
        var response = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        ClinicSlotUpdateRequest request = new ClinicSlotUpdateRequest(DayOfWeek.FRIDAY, "15:00", "17:00", 12);

        mockMvc.perform(patch("/api/v1/clinic-slots/{slotId}", response.id())
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.data.capacity").value(12));
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot을 삭제할 수 있다")
    void shouldDeleteSlot_whenValidOwner() throws Exception {
        var response = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        mockMvc.perform(delete("/api/v1/clinic-slots/{slotId}", response.id())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 ClinicSlot 삭제 시 404 응답을 반환한다")
    void shouldReturn404_whenDeleteNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/clinic-slots/{slotId}", UUID.randomUUID())
                        .with(teacherPrincipal()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot을 비활성화할 수 있다")
    void shouldDeactivateSlot() throws Exception {
        var response = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );

        mockMvc.perform(patch("/api/v1/clinic-slots/{slotId}/deactivate", response.id())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Teacher는 ClinicSlot을 활성화할 수 있다")
    void shouldActivateSlot() throws Exception {
        var response = clinicSlotService.createSlot(
                teacher.getId(),
                new ClinicSlotCreateRequest(DayOfWeek.MONDAY, "14:00", "16:00", 10)
        );
        clinicSlotService.deactivateSlot(teacher.getId(), response.id());

        mockMvc.perform(patch("/api/v1/clinic-slots/{slotId}/activate", response.id())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor teacherPrincipal() {
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                new MemberPrincipal(teacher.getId()),
                null,
                "TEACHER"
        );
        authenticationToken.setAuthenticated(true);
        return authentication(authenticationToken);
    }

    private RequestPostProcessor assistantPrincipal() {
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                new MemberPrincipal(assistant.getId()),
                null,
                "ASSISTANT"
        );
        authenticationToken.setAuthenticated(true);
        return authentication(authenticationToken);
    }

    private String toJson(ClinicSlotCreateRequest request) {
        return "{"
                + "\"dayOfWeek\":\"" + request.dayOfWeek() + "\""
                + ",\"startTime\":\"" + request.startTime() + "\""
                + ",\"endTime\":\"" + request.endTime() + "\""
                + ",\"capacity\":" + request.capacity()
                + "}";
    }

    private String toJson(ClinicSlotUpdateRequest request) {
        return "{"
                + "\"dayOfWeek\":" + quote(request.dayOfWeek())
                + ",\"startTime\":" + quote(request.startTime())
                + ",\"endTime\":" + quote(request.endTime())
                + ",\"capacity\":" + quote(request.capacity())
                + "}";
    }

    private String quote(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + value + "\"";
    }
}
