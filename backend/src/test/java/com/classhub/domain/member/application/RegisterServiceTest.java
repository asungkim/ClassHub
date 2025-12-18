package com.classhub.domain.member.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.member.dto.request.RegisterTeacherRequest;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private RegisterService registerService;

    private RegisterTeacherRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterTeacherRequest(
                "Teacher@ClassHub.com ",
                "Classhub!1",
                " Teacher Kim ",
                "010-1234-5678"
        );
    }

    @Test
    void registerTeacher_shouldCreateMemberAndIssueTokens() {
        given(memberRepository.findByEmail("teacher@classhub.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("Classhub!1")).willReturn("encoded-password");
        AuthTokens tokens = new AuthTokens(
                UUID.randomUUID(),
                "access-token",
                LocalDateTime.now().plusMinutes(30),
                "refresh-token",
                LocalDateTime.now().plusDays(7)
        );
        given(authService.login(any(LoginRequest.class))).willReturn(tokens);

        AuthTokens result = registerService.registerTeacher(request);

        assertThat(result).isEqualTo(tokens);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member saved = memberCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("teacher@classhub.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(MemberRole.TEACHER);
        assertThat(saved.getPhoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    void registerTeacher_shouldFail_whenEmailAlreadyExists() {
        Member existing = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher")
                .phoneNumber("010-0000-0000")
                .role(MemberRole.TEACHER)
                .build();
        given(memberRepository.findByEmail("teacher@classhub.com")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> registerService.registerTeacher(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerTeacher_shouldFail_whenEmailBelongsToInactiveMember() {
        Member inactive = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher")
                .phoneNumber("010-0000-0000")
                .role(MemberRole.TEACHER)
                .build();
        inactive.deactivate();
        given(memberRepository.findByEmail("teacher@classhub.com")).willReturn(Optional.of(inactive));

        assertThatThrownBy(() -> registerService.registerTeacher(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerTeacher_shouldNormalizeInputsBeforeSaving() {
        given(memberRepository.findByEmail("teacher@classhub.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("Classhub!1")).willReturn("encoded-password");
        given(authService.login(any(LoginRequest.class))).willReturn(
                new AuthTokens(
                        UUID.randomUUID(),
                        "access-token",
                        LocalDateTime.now().plusMinutes(30),
                        "refresh-token",
                        LocalDateTime.now().plusDays(7)
                )
        );

        registerService.registerTeacher(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member saved = memberCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("teacher@classhub.com");
        assertThat(saved.getName()).isEqualTo("Teacher Kim");
    }
}
