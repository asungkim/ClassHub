package com.classhub.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.auth.dto.TeacherRegisterRequest;
import com.classhub.domain.auth.dto.TeacherRegisterResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("Teacher 계정을 생성하면 암호화된 패스워드와 Role=TEACHER로 저장된다")
    void registerTeacher_success() {
        TeacherRegisterRequest request = new TeacherRegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        );

        TeacherRegisterResponse response = authService.registerTeacher(request);

        assertThat(response.email()).isEqualTo("teacher@classhub.com");
        Member saved = memberRepository.findById(response.memberId()).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.TEACHER);
        assertThat(passwordEncoder.matches("Classhub!1", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 등록된 이메일이면 BusinessException(DUPLICATE_EMAIL)을 던진다")
    void registerTeacher_duplicateEmail() {
        Member existing = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("선생님")
                .role(MemberRole.TEACHER)
                .build();
        memberRepository.save(existing);

        TeacherRegisterRequest request = new TeacherRegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        );

        assertThatThrownBy(() -> authService.registerTeacher(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.DUPLICATE_EMAIL.getMessage());
    }
}
