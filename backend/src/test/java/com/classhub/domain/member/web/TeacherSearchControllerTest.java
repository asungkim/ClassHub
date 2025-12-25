package com.classhub.domain.member.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.member.application.TeacherSearchService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class TeacherSearchControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private TeacherSearchService teacherSearchService;

    private MemberPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);
    }

    @Test
    void searchTeachers_shouldReturnPagedResults() throws Exception {
        UUID teacherId = UUID.randomUUID();
        TeacherSearchResponse response = new TeacherSearchResponse(
                teacherId,
                "Teacher Kim",
                List.of(new TeacherSearchResponse.TeacherBranchSummary(
                        UUID.randomUUID(),
                        "러셀",
                        UUID.randomUUID(),
                        "강남"
                ))
        );
        PageResponse<TeacherSearchResponse> page = new PageResponse<>(
                List.of(response),
                0,
                20,
                1,
                1,
                true,
                true
        );
        given(teacherSearchService.searchTeachers(eq(studentPrincipal.id()), any(), any(), any(), eq(0), eq(20)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/teachers")
                        .param("keyword", "kim")
                        .param("page", "0")
                        .param("size", "20")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].teacherId").value(teacherId.toString()));

        verify(teacherSearchService).searchTeachers(eq(studentPrincipal.id()), any(), any(), any(), eq(0), eq(20));
    }

    private RequestPostProcessor auth() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        studentPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority(studentPrincipal.role().name()))
                )
        );
    }
}
