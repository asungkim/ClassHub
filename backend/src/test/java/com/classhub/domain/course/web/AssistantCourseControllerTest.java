package com.classhub.domain.course.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.application.AssistantCourseService;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseWithTeacherResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AssistantCourseControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AssistantCourseService assistantCourseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private UsernamePasswordAuthenticationToken assistantToken() {
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.ASSISTANT);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ASSISTANT"))
        );
    }

    @Test
    void getCourses_shouldReturnAssistantView() throws Exception {
        PageResponse<CourseWithTeacherResponse> page = new PageResponse<>(
                List.of(new CourseWithTeacherResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "지점",
                        UUID.randomUUID(),
                        "회사",
                        "영어",
                        null,
                        LocalDate.now(),
                        LocalDate.now().plusDays(10),
                        true,
                        List.of(),
                        UUID.randomUUID(),
                        "Teacher Han"
                )),
                0,
                10,
                1,
                1,
                true,
                true
        );
        Mockito.when(assistantCourseService.getCourses(any(), any(), eq(CourseStatusFilter.ALL), any(), eq(0), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/assistants/me/courses")
                        .with(authentication(assistantToken()))
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].teacherName").value("Teacher Han"));

        verify(assistantCourseService).getCourses(any(), any(), eq(CourseStatusFilter.ALL), any(), eq(0), eq(10));
    }
}
