package com.classhub.domain.course.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.application.AdminCourseService;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.response.CourseResponse;
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
class AdminCourseControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AdminCourseService adminCourseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private UsernamePasswordAuthenticationToken superAdminToken() {
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
    }

    @Test
    void getCourses_shouldReturnAdminResults() throws Exception {
        PageResponse<CourseResponse> page = new PageResponse<>(
                List.of(new CourseResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "강남지점",
                        UUID.randomUUID(),
                        "클래스학원",
                        "토익",
                        null,
                        LocalDate.now(),
                        LocalDate.now().plusMonths(1),
                        true,
                        List.of()
                )),
                0,
                10,
                1,
                1,
                true,
                true
        );
        Mockito.when(adminCourseService.searchCourses(any(), any(), any(), eq(CourseStatusFilter.ALL), any(), eq(0), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/courses")
                        .with(authentication(superAdminToken()))
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].name").value("토익"));

        verify(adminCourseService).searchCourses(any(), any(), any(), eq(CourseStatusFilter.ALL), any(), eq(0), eq(10));
    }

    @Test
    void deleteCourse_shouldInvokeService() throws Exception {
        UUID courseId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}", courseId)
                        .with(authentication(superAdminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(adminCourseService).deleteCourse(courseId);
    }
}
