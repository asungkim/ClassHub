package com.classhub.domain.assignment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.assignment.application.TeacherBranchAssignmentService;
import com.classhub.domain.assignment.dto.TeacherBranchAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentCreateRequest.AssignmentCreationMode;
import com.classhub.domain.assignment.dto.request.TeacherBranchAssignmentStatusUpdateRequest;
import com.classhub.domain.assignment.dto.response.TeacherBranchAssignmentResponse;
import com.classhub.domain.assignment.model.BranchRole;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
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
class TeacherBranchAssignmentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeacherBranchAssignmentService teacherBranchAssignmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getAssignments_shouldReturnPagedResponse() throws Exception {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        TeacherBranchAssignmentResponse item = new TeacherBranchAssignmentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                CompanyType.ACADEMY,
                VerifiedStatus.VERIFIED,
                BranchRole.FREELANCE,
                LocalDateTime.now(),
                null
        );
        PageResponse<TeacherBranchAssignmentResponse> page = new PageResponse<>(
                List.of(item),
                0,
                20,
                1,
                1,
                true,
                true
        );
        org.mockito.BDDMockito.given(teacherBranchAssignmentService.getAssignments(
                eq(teacherId),
                eq(TeacherBranchAssignmentStatusFilter.ACTIVE),
                eq(0),
                eq(20)
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/teachers/me/branches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].branchName").value("강남"));

        verify(teacherBranchAssignmentService).getAssignments(
                teacherId,
                TeacherBranchAssignmentStatusFilter.ACTIVE,
                0,
                20
        );
    }

    @Test
    void createAssignment_shouldReturnCreatedResponse() throws Exception {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        UUID branchId = UUID.randomUUID();
        TeacherBranchAssignmentResponse response = new TeacherBranchAssignmentResponse(
                UUID.randomUUID(),
                branchId,
                "강남",
                UUID.randomUUID(),
                "러셀",
                CompanyType.ACADEMY,
                VerifiedStatus.VERIFIED,
                BranchRole.FREELANCE,
                LocalDateTime.now(),
                null
        );
        org.mockito.BDDMockito.given(teacherBranchAssignmentService.createAssignment(
                eq(teacherId),
                any(TeacherBranchAssignmentCreateRequest.class)
        )).willReturn(response);

        mockMvc.perform(post("/api/v1/teachers/me/branches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeacherBranchAssignmentCreateRequest(
                                AssignmentCreationMode.EXISTING_BRANCH,
                                branchId,
                                null,
                                null,
                                null,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.branchId").value(branchId.toString()));

        verify(teacherBranchAssignmentService).createAssignment(
                eq(teacherId),
                any(TeacherBranchAssignmentCreateRequest.class)
        );
    }

    @Test
    void updateAssignmentStatus_shouldInvokeService() throws Exception {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        UUID assignmentId = UUID.randomUUID();
        TeacherBranchAssignmentResponse response = new TeacherBranchAssignmentResponse(
                assignmentId,
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                CompanyType.ACADEMY,
                VerifiedStatus.VERIFIED,
                BranchRole.FREELANCE,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now()
        );
        org.mockito.BDDMockito.given(teacherBranchAssignmentService.updateAssignmentStatus(
                eq(teacherId),
                eq(assignmentId),
                any(TeacherBranchAssignmentStatusUpdateRequest.class)
        )).willReturn(response);

        mockMvc.perform(patch("/api/v1/teachers/me/branches/{assignmentId}", assignmentId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeacherBranchAssignmentStatusUpdateRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.assignmentId").value(assignmentId.toString()));

        verify(teacherBranchAssignmentService).updateAssignmentStatus(
                eq(teacherId),
                eq(assignmentId),
                any(TeacherBranchAssignmentStatusUpdateRequest.class)
        );
    }
}
