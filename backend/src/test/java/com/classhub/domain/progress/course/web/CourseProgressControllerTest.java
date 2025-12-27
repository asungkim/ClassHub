package com.classhub.domain.progress.course.web;

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

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.course.application.CourseProgressService;
import com.classhub.domain.progress.course.dto.request.CourseProgressComposeRequest;
import com.classhub.domain.progress.course.dto.request.CourseProgressCreateRequest;
import com.classhub.domain.progress.course.dto.request.CourseProgressUpdateRequest;
import com.classhub.domain.progress.course.dto.response.CourseProgressResponse;
import com.classhub.domain.progress.dto.ProgressSliceResponse;
import com.classhub.domain.progress.dto.ProgressSliceResponse.ProgressCursor;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressComposeRequest;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class CourseProgressControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseProgressService courseProgressService;

    private MockMvc mockMvc;
    private MemberPrincipal teacherPrincipal;
    private MemberPrincipal assistantPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        assistantPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.ASSISTANT);
    }

    @Test
    void createCourseProgress_shouldReturnCreated() throws Exception {
        CourseProgressResponse response = sampleResponse();
        given(courseProgressService.createCourseProgress(eq(teacherPrincipal), eq(response.courseId()), any()))
                .willReturn(response);

        CourseProgressCreateRequest request = new CourseProgressCreateRequest(
                response.date(),
                response.title(),
                response.content()
        );

        mockMvc.perform(post("/api/v1/courses/{courseId}/course-progress", response.courseId())
                        .with(auth(teacherPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.id").value(response.id().toString()));

        verify(courseProgressService).createCourseProgress(eq(teacherPrincipal), eq(response.courseId()), any());
    }

    @Test
    void createCourseProgress_shouldAllowAssistant() throws Exception {
        CourseProgressResponse response = sampleResponse();
        given(courseProgressService.createCourseProgress(eq(assistantPrincipal), eq(response.courseId()), any()))
                .willReturn(response);

        CourseProgressCreateRequest request = new CourseProgressCreateRequest(
                response.date(),
                response.title(),
                response.content()
        );

        mockMvc.perform(post("/api/v1/courses/{courseId}/course-progress", response.courseId())
                        .with(auth(assistantPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()));

        verify(courseProgressService).createCourseProgress(eq(assistantPrincipal), eq(response.courseId()), any());
    }

    @Test
    void composeCourseProgress_shouldReturnCreated() throws Exception {
        CourseProgressResponse response = sampleResponse();
        UUID recordId = UUID.randomUUID();
        CourseProgressComposeRequest request = new CourseProgressComposeRequest(
                new CourseProgressCreateRequest(response.date(), response.title(), response.content()),
                List.of(new PersonalProgressComposeRequest(recordId, response.date(), "개별", "memo"))
        );
        given(courseProgressService.composeCourseProgress(eq(teacherPrincipal), eq(response.courseId()), any()))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/courses/{courseId}/course-progress/compose", response.courseId())
                        .with(auth(teacherPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.id").value(response.id().toString()));

        verify(courseProgressService).composeCourseProgress(eq(teacherPrincipal), eq(response.courseId()), any());
    }

    @Test
    void getCourseProgresses_shouldReturnSliceForAssistant() throws Exception {
        CourseProgressResponse response = sampleResponse();
        ProgressSliceResponse<CourseProgressResponse> slice = new ProgressSliceResponse<>(
                List.of(response),
                new ProgressCursor(response.id(), response.createdAt())
        );
        given(courseProgressService.getCourseProgresses(
                eq(assistantPrincipal),
                eq(response.courseId()),
                any(),
                any(),
                any()
        )).willReturn(slice);

        mockMvc.perform(get("/api/v1/courses/{courseId}/course-progress", response.courseId())
                        .with(auth(assistantPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value(response.title()));
    }

    @Test
    void updateCourseProgress_shouldReturnUpdated() throws Exception {
        CourseProgressResponse response = sampleResponse();
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 10),
                "Updated",
                "memo"
        );
        CourseProgressResponse updated = new CourseProgressResponse(
                response.id(),
                response.courseId(),
                request.date(),
                request.title(),
                request.content(),
                response.writerId(),
                response.writerName(),
                response.writerRole(),
                response.createdAt()
        );
        given(courseProgressService.updateCourseProgress(eq(teacherPrincipal), eq(response.id()), any()))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/course-progress/{progressId}", response.id())
                        .with(auth(teacherPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void updateCourseProgress_shouldAllowAssistant() throws Exception {
        CourseProgressResponse response = sampleResponse();
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 10),
                "Updated",
                "memo"
        );
        CourseProgressResponse updated = new CourseProgressResponse(
                response.id(),
                response.courseId(),
                request.date(),
                request.title(),
                request.content(),
                response.writerId(),
                response.writerName(),
                response.writerRole(),
                response.createdAt()
        );
        given(courseProgressService.updateCourseProgress(eq(assistantPrincipal), eq(response.id()), any()))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/course-progress/{progressId}", response.id())
                        .with(auth(assistantPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void deleteCourseProgress_shouldReturnSuccess() throws Exception {
        UUID progressId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/course-progress/{progressId}", progressId)
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(courseProgressService).deleteCourseProgress(teacherPrincipal, progressId);
    }

    private CourseProgressResponse sampleResponse() {
        return new CourseProgressResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2024, Month.MARCH, 5),
                "Lesson",
                "memo",
                teacherPrincipal.id(),
                "테스트 선생님",
                MemberRole.TEACHER,
                LocalDateTime.of(2024, Month.MARCH, 5, 10, 0)
        );
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
