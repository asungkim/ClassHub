package com.classhub.domain.studentcourse.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.application.CourseAssignmentService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.dto.request.StudentCourseAssignmentCreateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseAssignmentResponse;
import com.classhub.global.response.RsCode;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class StudentCourseAssignmentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseAssignmentService courseAssignmentService;

    private MockMvc mockMvc;
    private MemberPrincipal teacherPrincipal;
    private UsernamePasswordAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        authenticationToken = new UsernamePasswordAuthenticationToken(
                teacherPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
    }

    @Test
    void createAssignment_shouldReturnCreated() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        StudentCourseAssignmentResponse response = new StudentCourseAssignmentResponse(
                assignmentId,
                studentId,
                courseId,
                teacherPrincipal.id(),
                LocalDateTime.now(),
                true
        );
        given(courseAssignmentService.createAssignment(eq(teacherPrincipal), any(StudentCourseAssignmentCreateRequest.class)))
                .willReturn(response);

        StudentCourseAssignmentCreateRequest request = new StudentCourseAssignmentCreateRequest(studentId, courseId);

        mockMvc.perform(post("/api/v1/student-course-assignments")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId.toString()));

        verify(courseAssignmentService).createAssignment(eq(teacherPrincipal), any(StudentCourseAssignmentCreateRequest.class));
    }
}
