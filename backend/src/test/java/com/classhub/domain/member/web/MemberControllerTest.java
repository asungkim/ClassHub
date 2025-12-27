package com.classhub.domain.member.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.member.application.MemberProfileService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.request.MemberProfileUpdateRequest;
import com.classhub.domain.member.dto.response.MemberProfileResponse;
import com.classhub.domain.member.dto.response.MemberProfileResponse.MemberProfileInfo;
import com.classhub.domain.member.dto.response.MemberProfileResponse.StudentInfoResponse;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class MemberControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberProfileService memberProfileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getProfile_shouldReturnMemberProfile() throws Exception {
        UUID memberId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(memberId, MemberRole.TEACHER);
        MemberProfileResponse response = new MemberProfileResponse(
                new MemberProfileInfo(memberId, "teacher@classhub.dev", "Teacher", "010-1234-5678", MemberRole.TEACHER),
                null
        );

        given(memberProfileService.getProfile(eq(memberId))).willReturn(response);

        mockMvc.perform(get("/api/v1/members/me")
                        .with(auth(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.member.email").value("teacher@classhub.dev"))
                .andExpect(jsonPath("$.data.member.role").value("TEACHER"));
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() throws Exception {
        UUID memberId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(memberId, MemberRole.STUDENT);
        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
                null,
                "New Name",
                null,
                null,
                null
        );
        MemberProfileResponse response = new MemberProfileResponse(
                new MemberProfileInfo(memberId, "student@classhub.dev", "New Name", "010-0000-0000", MemberRole.STUDENT),
                new StudentInfoResponse("School", StudentGrade.HIGH_2, LocalDate.of(2008, 3, 1), "010-9999-0000")
        );

        given(memberProfileService.updateProfile(eq(memberId), eq(request))).willReturn(response);

        mockMvc.perform(put("/api/v1/members/me")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(auth(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.member.name").value("New Name"))
                .andExpect(jsonPath("$.data.studentInfo.grade").value("HIGH_2"));
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
