package com.classhub.domain.company.company.web;

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

import com.classhub.domain.company.company.application.CompanyCommandService;
import com.classhub.domain.company.company.application.CompanyQueryService;
import com.classhub.domain.company.company.dto.request.CompanyCreateRequest;
import com.classhub.domain.company.company.dto.request.CompanyVerifiedStatusRequest;
import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
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
class CompanyControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyCommandService companyCommandService;

    @MockitoBean
    private CompanyQueryService companyQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createCompany_shouldRequireTeacherAuthority() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        CompanyResponse response = new CompanyResponse(
                companyId,
                "Alice Lab",
                "desc",
                CompanyType.INDIVIDUAL,
                VerifiedStatus.VERIFIED,
                teacherId,
                LocalDateTime.now(),
                null
        );
        given(companyCommandService.createCompany(eq(teacherId), any(CompanyCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/companies")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompanyCreateRequest(
                                "Alice Lab",
                                "desc",
                                CompanyType.INDIVIDUAL,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()));

        verify(companyCommandService).createCompany(eq(teacherId), any(CompanyCreateRequest.class));
    }

    @Test
    void getCompaniesForTeacher_shouldReturnPagedResponse() throws Exception {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        PageResponse<CompanyResponse> page = new PageResponse<>(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );
        given(companyQueryService.getCompaniesForTeacher(eq(teacherId), eq(VerifiedStatus.VERIFIED), eq(CompanyType.ACADEMY), eq("러셀"), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/companies")
                        .param("status", "VERIFIED")
                        .param("type", "ACADEMY")
                        .param("keyword", "러셀")
                        .param("page", "0")
                        .param("size", "20")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(companyQueryService).getCompaniesForTeacher(eq(teacherId), eq(VerifiedStatus.VERIFIED), eq(CompanyType.ACADEMY), eq("러셀"), any());
    }

    @Test
    void getCompaniesForAdmin_shouldReturnPagedResponse() throws Exception {
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        PageResponse<CompanyResponse> page = new PageResponse<>(List.of(), 0, 10, 0, 0, true, true);
        given(companyQueryService.getCompaniesForAdmin(eq(VerifiedStatus.UNVERIFIED), eq(null), eq(null), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/admin/companies")
                        .param("status", "UNVERIFIED")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(companyQueryService).getCompaniesForAdmin(eq(VerifiedStatus.UNVERIFIED), eq(null), eq(null), any());
    }

    @Test
    void getCompanyDetail_shouldReturnResponse() throws Exception {
        UUID companyId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        CompanyResponse response = new CompanyResponse(
                companyId,
                "러셀",
                null,
                CompanyType.ACADEMY,
                VerifiedStatus.UNVERIFIED,
                UUID.randomUUID(),
                LocalDateTime.now(),
                null
        );
        given(companyQueryService.getCompany(companyId)).willReturn(response);

        mockMvc.perform(get("/api/v1/admin/companies/{companyId}", companyId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()));

        verify(companyQueryService).getCompany(companyId);
    }

    @Test
    void updateCompanyVerifiedStatus_shouldInvokeCommandService() throws Exception {
        UUID companyId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        CompanyResponse response = new CompanyResponse(
                companyId,
                "러셀",
                null,
                CompanyType.ACADEMY,
                VerifiedStatus.VERIFIED,
                UUID.randomUUID(),
                LocalDateTime.now(),
                null
        );
        given(companyCommandService.updateCompanyVerifiedStatus(eq(companyId), any(CompanyVerifiedStatusRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/admin/companies/{companyId}/verified-status", companyId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompanyVerifiedStatusRequest(true, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(companyCommandService).updateCompanyVerifiedStatus(eq(companyId), any(CompanyVerifiedStatusRequest.class));
    }
}
