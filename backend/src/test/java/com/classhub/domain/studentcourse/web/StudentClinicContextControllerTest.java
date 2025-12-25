package com.classhub.domain.studentcourse.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.application.StudentClinicContextQueryService;
import com.classhub.domain.studentcourse.dto.response.StudentClinicContextResponse;
import com.classhub.global.response.RsCode;
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
class StudentClinicContextControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StudentClinicContextQueryService queryService;

    private MockMvc mockMvc;
    private MemberPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);
    }

    @Test
    void getClinicContexts_shouldReturnList() throws Exception {
        StudentClinicContextResponse response = new StudentClinicContextResponse(
                UUID.randomUUID(),
                "중3 수학",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Teacher",
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀"
        );
        given(queryService.getContexts(eq(studentPrincipal.id())))
                .willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/students/me/clinic-contexts")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].courseName").value("중3 수학"))
                .andExpect(jsonPath("$.data[0].teacherName").value("Teacher"))
                .andExpect(jsonPath("$.data[0].companyName").value("러셀"));

        verify(queryService).getContexts(studentPrincipal.id());
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                studentPrincipal,
                null,
                List.of(new SimpleGrantedAuthority(studentPrincipal.role().name()))
        );
    }
}
