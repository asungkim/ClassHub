package com.classhub.domain.company.branch.web;

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

import com.classhub.domain.company.branch.application.BranchCommandService;
import com.classhub.domain.company.branch.application.BranchQueryService;
import com.classhub.domain.company.branch.dto.request.BranchCreateRequest;
import com.classhub.domain.company.branch.dto.request.BranchUpdateRequest;
import com.classhub.domain.company.branch.dto.request.BranchVerifiedStatusRequest;
import com.classhub.domain.company.branch.dto.response.BranchResponse;
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

@SpringBootTest
@ActiveProfiles("test")
class BranchControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private BranchCommandService branchCommandService;

    @MockitoBean
    private BranchQueryService branchQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createBranch_shouldReturnCreatedResponse() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        BranchResponse response = new BranchResponse(
                branchId,
                UUID.randomUUID(),
                "강남",
                VerifiedStatus.UNVERIFIED,
                teacherId,
                LocalDateTime.now(),
                null
        );
        given(branchCommandService.createBranch(eq(teacherId), any(BranchCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/branches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "%s",
                                  "name": "강남"
                                }
                                """.formatted(response.companyId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.branchId").value(branchId.toString()));

        verify(branchCommandService).createBranch(eq(teacherId), any(BranchCreateRequest.class));
    }

    @Test
    void getBranches_shouldCallTeacherService() throws Exception {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        PageResponse<BranchResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        given(branchQueryService.getBranchesForTeacher(eq(teacherId), eq(null), eq(VerifiedStatus.VERIFIED), eq("강남"), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/branches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .param("status", "VERIFIED")
                        .param("keyword", "강남"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(branchQueryService).getBranchesForTeacher(eq(teacherId), eq(null), eq(VerifiedStatus.VERIFIED), eq("강남"), any());
    }

    @Test
    void getBranches_shouldCallAdminService() throws Exception {
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        PageResponse<BranchResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        given(branchQueryService.getBranchesForAdmin(eq(null), eq(VerifiedStatus.UNVERIFIED), eq(null), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/branches")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .param("status", "UNVERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(branchQueryService).getBranchesForAdmin(eq(null), eq(VerifiedStatus.UNVERIFIED), eq(null), any());
    }

    @Test
    void updateBranch_shouldInvokeCommandService() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        BranchResponse response = new BranchResponse(
                branchId,
                UUID.randomUUID(),
                "강남",
                VerifiedStatus.UNVERIFIED,
                teacherId,
                LocalDateTime.now(),
                null
        );
        given(branchCommandService.updateBranch(eq(teacherId), eq(MemberRole.TEACHER), eq(branchId), any(BranchUpdateRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/branches/{branchId}", branchId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "잠실",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(branchCommandService).updateBranch(eq(teacherId), eq(MemberRole.TEACHER), eq(branchId), any(BranchUpdateRequest.class));
    }

    @Test
    void updateBranchVerifiedStatus_shouldRequireSuperAdmin() throws Exception {
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        BranchResponse response = new BranchResponse(
                branchId,
                UUID.randomUUID(),
                "강남",
                VerifiedStatus.VERIFIED,
                principal.id(),
                LocalDateTime.now(),
                null
        );
        given(branchCommandService.updateBranchVerifiedStatus(eq(branchId), any(BranchVerifiedStatusRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/branches/{branchId}/verified-status", branchId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "verified": true,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(branchCommandService).updateBranchVerifiedStatus(eq(branchId), any(BranchVerifiedStatusRequest.class));
    }
}
