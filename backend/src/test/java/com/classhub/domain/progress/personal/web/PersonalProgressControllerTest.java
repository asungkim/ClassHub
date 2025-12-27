package com.classhub.domain.progress.personal.web;

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
import com.classhub.domain.progress.dto.ProgressSliceResponse;
import com.classhub.domain.progress.dto.ProgressSliceResponse.ProgressCursor;
import com.classhub.domain.progress.personal.application.PersonalProgressService;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressCreateRequest;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressUpdateRequest;
import com.classhub.domain.progress.personal.dto.response.PersonalProgressResponse;
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
class PersonalProgressControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PersonalProgressService personalProgressService;

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
    void createPersonalProgress_shouldReturnCreated() throws Exception {
        PersonalProgressResponse response = sampleResponse();
        given(personalProgressService.createPersonalProgress(eq(teacherPrincipal), eq(response.studentCourseRecordId()), any()))
                .willReturn(response);

        PersonalProgressCreateRequest request = new PersonalProgressCreateRequest(
                response.date(),
                response.title(),
                response.content()
        );

        mockMvc.perform(post("/api/v1/student-courses/{recordId}/personal-progress", response.studentCourseRecordId())
                        .with(auth(teacherPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.id").value(response.id().toString()));

        verify(personalProgressService).createPersonalProgress(eq(teacherPrincipal), eq(response.studentCourseRecordId()), any());
    }

    @Test
    void createPersonalProgress_shouldAllowAssistant() throws Exception {
        PersonalProgressResponse response = sampleResponse();
        given(personalProgressService.createPersonalProgress(eq(assistantPrincipal), eq(response.studentCourseRecordId()), any()))
                .willReturn(response);

        PersonalProgressCreateRequest request = new PersonalProgressCreateRequest(
                response.date(),
                response.title(),
                response.content()
        );

        mockMvc.perform(post("/api/v1/student-courses/{recordId}/personal-progress", response.studentCourseRecordId())
                        .with(auth(assistantPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()));

        verify(personalProgressService).createPersonalProgress(eq(assistantPrincipal), eq(response.studentCourseRecordId()), any());
    }

    @Test
    void getPersonalProgresses_shouldReturnSliceForAssistant() throws Exception {
        PersonalProgressResponse response = sampleResponse();
        ProgressSliceResponse<PersonalProgressResponse> slice = new ProgressSliceResponse<>(
                List.of(response),
                new ProgressCursor(response.id(), response.createdAt())
        );
        given(personalProgressService.getPersonalProgresses(
                eq(assistantPrincipal),
                eq(response.studentCourseRecordId()),
                any(),
                any(),
                any()
        )).willReturn(slice);

        mockMvc.perform(get("/api/v1/student-courses/{recordId}/personal-progress", response.studentCourseRecordId())
                        .with(auth(assistantPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value(response.title()));
    }

    @Test
    void updatePersonalProgress_shouldReturnUpdated() throws Exception {
        PersonalProgressResponse response = sampleResponse();
        PersonalProgressUpdateRequest request = new PersonalProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 10),
                "Updated",
                "memo"
        );
        PersonalProgressResponse updated = new PersonalProgressResponse(
                response.id(),
                response.studentCourseRecordId(),
                response.courseId(),
                request.date(),
                request.title(),
                request.content(),
                response.writerId(),
                response.writerName(),
                response.writerRole(),
                response.createdAt()
        );
        given(personalProgressService.updatePersonalProgress(eq(teacherPrincipal), eq(response.id()), any()))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/personal-progress/{progressId}", response.id())
                        .with(auth(teacherPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void updatePersonalProgress_shouldAllowAssistant() throws Exception {
        PersonalProgressResponse response = sampleResponse();
        PersonalProgressUpdateRequest request = new PersonalProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 10),
                "Updated",
                "memo"
        );
        PersonalProgressResponse updated = new PersonalProgressResponse(
                response.id(),
                response.studentCourseRecordId(),
                response.courseId(),
                request.date(),
                request.title(),
                request.content(),
                response.writerId(),
                response.writerName(),
                response.writerRole(),
                response.createdAt()
        );
        given(personalProgressService.updatePersonalProgress(eq(assistantPrincipal), eq(response.id()), any()))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/personal-progress/{progressId}", response.id())
                        .with(auth(assistantPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void deletePersonalProgress_shouldReturnSuccess() throws Exception {
        UUID progressId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/personal-progress/{progressId}", progressId)
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(personalProgressService).deletePersonalProgress(teacherPrincipal, progressId);
    }

    private PersonalProgressResponse sampleResponse() {
        return new PersonalProgressResponse(
                UUID.randomUUID(),
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
