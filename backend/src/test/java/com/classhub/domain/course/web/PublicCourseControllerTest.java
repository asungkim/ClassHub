package com.classhub.domain.course.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.classhub.domain.course.application.PublicCourseService;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.course.dto.response.PublicCourseResponse;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PublicCourseControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PublicCourseService publicCourseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getPublicCourses_shouldReturnResults() throws Exception {
        PageResponse<PublicCourseResponse> page = new PageResponse<>(
                List.of(new PublicCourseResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "강남",
                        UUID.randomUUID(),
                        "클래스",
                        "국어",
                        "심화",
                        LocalDate.now(),
                        LocalDate.now().plusMonths(1),
                        true,
                        List.of(new CourseScheduleResponse(
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(11, 0)
                        )),
                        UUID.randomUUID(),
                        "Teacher Kim",
                        "월 09:00~11:00"
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );
        Mockito.when(publicCourseService.searchCourses(any(), any(), any(), any(), eq(false), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/public")
                        .param("onlyVerified", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].teacherName").value("Teacher Kim"));

        verify(publicCourseService).searchCourses(any(), any(), any(), any(), eq(false), eq(0), eq(20));
    }
}
