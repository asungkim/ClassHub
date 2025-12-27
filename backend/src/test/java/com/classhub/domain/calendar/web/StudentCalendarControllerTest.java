package com.classhub.domain.calendar.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.calendar.application.StudentCalendarService;
import com.classhub.domain.calendar.dto.StudentCalendarResponse;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.ClinicEvent;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.ClinicRecordSummary;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.CourseProgressEvent;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.PersonalProgressEvent;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

@SpringBootTest
@ActiveProfiles("test")
class StudentCalendarControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StudentCalendarService studentCalendarService;

    private MockMvc mockMvc;
    private MemberPrincipal teacherPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
    }

    @Test
    void getStudentCalendar_shouldReturnResponse() throws Exception {
        UUID studentId = UUID.randomUUID();
        int year = 2024;
        int month = 3;
        CourseProgressEvent courseEvent = new CourseProgressEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "중3 수학",
                LocalDate.of(2024, Month.MARCH, 2),
                "3주차",
                "공통 내용",
                teacherPrincipal.id(),
                "Kim",
                MemberRole.TEACHER,
                LocalDateTime.of(2024, Month.MARCH, 2, 9, 0)
        );
        PersonalProgressEvent personalEvent = new PersonalProgressEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                courseEvent.courseId(),
                "중3 수학",
                LocalDate.of(2024, Month.MARCH, 3),
                "개별",
                "개인 내용",
                UUID.randomUUID(),
                "Lee",
                MemberRole.ASSISTANT,
                LocalDateTime.of(2024, Month.MARCH, 3, 10, 30)
        );
        ClinicRecordSummary recordSummary = new ClinicRecordSummary(
                UUID.randomUUID(),
                "클리닉",
                "클리닉 내용",
                "숙제",
                UUID.randomUUID(),
                "Park",
                MemberRole.TEACHER,
                LocalDateTime.of(2024, Month.MARCH, 4, 18, 30)
        );
        ClinicEvent clinicEvent = new ClinicEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                courseEvent.courseId(),
                UUID.randomUUID(),
                LocalDate.of(2024, Month.MARCH, 4),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                false,
                recordSummary
        );
        StudentCalendarResponse response = new StudentCalendarResponse(
                studentId,
                year,
                month,
                List.of(courseEvent),
                List.of(personalEvent),
                List.of(clinicEvent)
        );
        given(studentCalendarService.getStudentCalendar(eq(teacherPrincipal), eq(studentId), eq(year), eq(month)))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/students/{studentId}/calendar", studentId)
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month))
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.courseProgress[0].courseName").value("중3 수학"))
                .andExpect(jsonPath("$.data.courseProgress[0].content").value("공통 내용"))
                .andExpect(jsonPath("$.data.personalProgress[0].writerRole").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.personalProgress[0].content").value("개인 내용"))
                .andExpect(jsonPath("$.data.clinicEvents[0].recordSummary.title").value("클리닉"))
                .andExpect(jsonPath("$.data.clinicEvents[0].recordSummary.content").value("클리닉 내용"));
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
