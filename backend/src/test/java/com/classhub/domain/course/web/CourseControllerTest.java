package com.classhub.domain.course.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.application.CourseService;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseScheduleRequest;
import com.classhub.domain.course.dto.request.CourseStatusUpdateRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    private MockMvc mockMvc;

    private MemberPrincipal principal;
    private UsernamePasswordAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
    }

    @Test
    void getCourses_shouldReturnPageResponse() throws Exception {
        CourseResponse response = sampleResponse();
        PageResponse<CourseResponse> page = new PageResponse<>(
                List.of(response),
                0,
                20,
                1,
                1,
                true,
                true
        );
        given(courseService.getCourses(
                eq(principal.id()),
                eq(CourseStatusFilter.ACTIVE),
                any(),
                any(),
                eq(0),
                eq(20)
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/courses")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].name").value("중3 수학"));

        verify(courseService).getCourses(
                principal.id(),
                CourseStatusFilter.ACTIVE,
                null,
                null,
                0,
                20
        );
    }

    @Test
    void createCourse_shouldReturnCreated() throws Exception {
        CourseResponse response = sampleResponse();
        given(courseService.createCourse(eq(principal.id()), any(CourseCreateRequest.class))).willReturn(response);

        CourseCreateRequest request = new CourseCreateRequest(
                response.branchId(),
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                List.of(new CourseScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0)))
        );

        mockMvc.perform(post("/api/v1/courses")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(response.courseId().toString()));

        verify(courseService).createCourse(eq(principal.id()), any(CourseCreateRequest.class));
    }

    @Test
    void getCourseSchedules_shouldReturnList() throws Exception {
        CourseResponse response = sampleResponse();
        given(courseService.getCoursesWithinPeriod(
                eq(principal.id()),
                any(LocalDate.class),
                any(LocalDate.class)
        )).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/courses/schedule")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].courseId").value(response.courseId().toString()));

        verify(courseService).getCoursesWithinPeriod(
                eq(principal.id()),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }

    @Test
    void getCourse_shouldReturnDetail() throws Exception {
        CourseResponse response = sampleResponse();
        UUID courseId = response.courseId();
        given(courseService.getCourse(principal.id(), courseId)).willReturn(response);

        mockMvc.perform(get("/api/v1/courses/{courseId}", courseId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()));

        verify(courseService).getCourse(principal.id(), courseId);
    }

    @Test
    void updateCourse_shouldInvokeService() throws Exception {
        CourseResponse response = sampleResponse();
        UUID courseId = response.courseId();
        given(courseService.updateCourse(eq(principal.id()), eq(courseId), any(CourseUpdateRequest.class)))
                .willReturn(response);

        CourseUpdateRequest request = new CourseUpdateRequest(
                "새 이름",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                List.of(new CourseScheduleRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
        );

        mockMvc.perform(patch("/api/v1/courses/{courseId}", courseId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(courseService).updateCourse(eq(principal.id()), eq(courseId), any(CourseUpdateRequest.class));
    }

    @Test
    void updateCourseStatus_shouldToggleCourse() throws Exception {
        CourseResponse response = sampleResponse();
        UUID courseId = response.courseId();
        given(courseService.updateCourseStatus(eq(principal.id()), eq(courseId), any(CourseStatusUpdateRequest.class)))
                .willReturn(response);

        mockMvc.perform(patch("/api/v1/courses/{courseId}/status", courseId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseStatusUpdateRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(courseService).updateCourseStatus(eq(principal.id()), eq(courseId), any(CourseStatusUpdateRequest.class));
    }

    private CourseResponse sampleResponse() {
        return new CourseResponse(
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
                List.of(new CourseScheduleResponse(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0)))
        );
    }
}
