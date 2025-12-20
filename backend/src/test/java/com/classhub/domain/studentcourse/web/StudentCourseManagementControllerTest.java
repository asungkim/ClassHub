package com.classhub.domain.studentcourse.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.application.StudentCourseManagementService;
import com.classhub.domain.studentcourse.dto.StudentCourseStatusFilter;
import com.classhub.domain.studentcourse.dto.request.StudentCourseRecordUpdateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse.StudentSummary;
import com.classhub.domain.studentcourse.dto.response.StudentCourseListItemResponse;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
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
class StudentCourseManagementControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StudentCourseManagementService managementService;

    private MockMvc mockMvc;
    private MemberPrincipal teacherPrincipal;
    private MemberPrincipal assistantPrincipal;
    private StudentCourseListItemResponse listItem;
    private StudentCourseDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        assistantPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.ASSISTANT);
        listItem = new StudentCourseListItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "홍길동",
                "010-0000-0000",
                "ClassHub 고등학교",
                "HIGH_2",
                18,
                UUID.randomUUID(),
                "고2 수학",
                true,
                null,
                null
        );
        detailResponse = new StudentCourseDetailResponse(
                listItem.recordId(),
                new StudentSummary(
                        listItem.studentMemberId(),
                        "홍길동",
                        "student@classhub.dev",
                        "010-0000-0000",
                        "ClassHub 고등학교",
                        "HIGH_2",
                        LocalDate.now().minusYears(18),
                        18,
                        "010-1111-2222"
                ),
                null,
                null,
                null,
                "메모",
                true
        );
    }

    @Test
    void getStudentCourses_shouldWorkForTeacher() throws Exception {
        PageResponse<StudentCourseListItemResponse> page = new PageResponse<>(
                List.of(listItem),
                0,
                20,
                1,
                1,
                true,
                true
        );
        given(managementService.getStudentCourses(
                eq(teacherPrincipal),
                any(),
                eq(StudentCourseStatusFilter.ACTIVE),
                eq("hong"),
                eq(0),
                eq(20)
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/student-courses")
                        .param("status", "ACTIVE")
                        .param("keyword", "hong")
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].studentName").value("홍길동"));

        verify(managementService).getStudentCourses(
                eq(teacherPrincipal),
                any(),
                eq(StudentCourseStatusFilter.ACTIVE),
                eq("hong"),
                eq(0),
                eq(20)
        );
    }

    @Test
    void getStudentCourses_shouldWorkForAssistant() throws Exception {
        PageResponse<StudentCourseListItemResponse> page = new PageResponse<>(
                List.of(listItem),
                0,
                10,
                1,
                1,
                true,
                true
        );
        given(managementService.getStudentCourses(
                eq(assistantPrincipal),
                any(),
                eq(StudentCourseStatusFilter.ACTIVE),
                eq(null),
                eq(0),
                eq(10)
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/student-courses")
                        .param("size", "10")
                        .with(auth(assistantPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].courseName").value("고2 수학"));
    }

    @Test
    void getStudentCourseDetail_shouldReturnData() throws Exception {
        given(managementService.getStudentCourseDetail(eq(teacherPrincipal.id()), eq(listItem.recordId())))
                .willReturn(detailResponse);

        mockMvc.perform(get("/api/v1/student-courses/{id}", listItem.recordId())
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(listItem.recordId().toString()));
    }

    @Test
    void updateStudentCourse_shouldReturnUpdated() throws Exception {
        StudentCourseRecordUpdateRequest request = new StudentCourseRecordUpdateRequest(
                UUID.randomUUID(),
                null,
                "새 메모"
        );
        StudentCourseDetailResponse updated = new StudentCourseDetailResponse(
                listItem.recordId(),
                detailResponse.student(),
                detailResponse.course(),
                request.assistantMemberId(),
                null,
                "새 메모",
                true
        );
        given(managementService.updateStudentCourseRecord(eq(teacherPrincipal.id()), eq(listItem.recordId()), any()))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/student-courses/{id}", listItem.recordId())
                        .with(auth(teacherPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "assistantMemberId": "%s",
                                  "teacherNotes": "새 메모"
                                }
                                """.formatted(request.assistantMemberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherNotes").value("새 메모"));
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
