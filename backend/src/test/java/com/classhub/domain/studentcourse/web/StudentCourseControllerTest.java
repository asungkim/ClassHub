package com.classhub.domain.studentcourse.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.clinic.clinicdefaultslot.application.ClinicDefaultSlotService;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.application.StudentCourseQueryService;
import com.classhub.domain.studentcourse.dto.request.StudentDefaultClinicSlotRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseResponse;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class StudentCourseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentCourseQueryService queryService;

    @MockitoBean
    private ClinicDefaultSlotService clinicDefaultSlotService;

    private MemberPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);
    }

    @Test
    void getMyCourses_shouldReturnPagedContent() throws Exception {
        CourseResponse courseResponse = new CourseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
        StudentCourseResponse response = new StudentCourseResponse(
                UUID.randomUUID(),
                LocalDateTime.now(),
                courseResponse
        );
        PageResponse<StudentCourseResponse> page = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                true,
                true
        );
        given(queryService.getMyCourses(
                eq(studentPrincipal.id()),
                anyString(),
                anyInt(),
                anyInt()
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/students/me/courses")
                        .with(auth())
                        .param("keyword", "수학")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].course.name").value("중3 수학"));

        verify(queryService).getMyCourses(studentPrincipal.id(), "수학", 0, 10);
    }

    @Test
    void updateDefaultClinicSlot_shouldReturnUpdatedSlot() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        StudentDefaultClinicSlotRequest request = new StudentDefaultClinicSlotRequest(slotId);
        StudentCourseRecord record = StudentCourseRecord.create(studentPrincipal.id(), courseId, null, slotId, null);
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());

        given(clinicDefaultSlotService.updateDefaultSlotForStudent(studentPrincipal.id(), courseId, slotId))
                .willReturn(record);

        mockMvc.perform(patch("/api/v1/students/me/courses/{courseId}/clinic-slot", courseId)
                        .with(auth())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.studentCourseRecordId").value(record.getId().toString()))
                .andExpect(jsonPath("$.data.defaultClinicSlotId").value(slotId.toString()));

        verify(clinicDefaultSlotService).updateDefaultSlotForStudent(studentPrincipal.id(), courseId, slotId);
    }

    private RequestPostProcessor auth() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        studentPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority(studentPrincipal.role().name()))
                )
        );
    }
}
