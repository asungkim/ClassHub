package com.classhub.domain.studentcourse.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.clinic.slot.application.ClinicDefaultSlotService;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.application.StudentCourseQueryService;
import com.classhub.domain.studentcourse.dto.response.StudentMyCourseResponse;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private StudentCourseQueryService queryService;

    @MockitoBean
    private ClinicDefaultSlotService clinicDefaultSlotService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getMyCourses_shouldReturnAssignments() throws Exception {
        UUID studentId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(studentId, MemberRole.STUDENT);

        UUID assignmentId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        CourseResponse course = new CourseResponse(
                courseId,
                UUID.randomUUID(),
                "Branch",
                UUID.randomUUID(),
                "Company",
                "Course",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 1),
                true,
                List.of()
        );
        StudentMyCourseResponse item = new StudentMyCourseResponse(
                assignmentId,
                null,
                true,
                recordId,
                course
        );
        PageResponse<StudentMyCourseResponse> response = PageResponse.from(
                new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1)
        );

        given(queryService.getMyCourses(studentId, 0, 20)).willReturn(response);

        mockMvc.perform(get("/api/v1/students/me/courses")
                        .contentType(APPLICATION_JSON)
                        .with(auth(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].assignmentId").value(assignmentId.toString()))
                .andExpect(jsonPath("$.data.content[0].recordId").value(recordId.toString()))
                .andExpect(jsonPath("$.data.content[0].course.courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.content[0].assignmentActive").value(true));
    }

    private RequestPostProcessor auth(MemberPrincipal principal) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(principal.role().name()))
                )
        );
    }
}
