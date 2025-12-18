package com.classhub.domain.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.dto.request.RegisterStudentRequest;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.exception.BusinessException;
import java.time.LocalDate;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StudentInfoRepository studentInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private RegisterService registerService;

    private RegisterMemberRequest request;
    private RegisterStudentRequest studentRequest;

    @BeforeEach
    void setUp() {
        request = new RegisterMemberRequest(
                "Teacher@ClassHub.com ",
                "Classhub!1",
                " Teacher Kim ",
                "010-1234-5678"
        );

        studentRequest = new RegisterStudentRequest(
                new RegisterMemberRequest(
                        " Student@classhub.com ",
                        "Classhub!1",
                        " Student Hong ",
                        "010-2222-3333"
                ),
                " 서울중학교 ",
                StudentGrade.MIDDLE_2,
                LocalDate.of(2010, 3, 15),
                "010-9999-8888"
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

    @Test
    void registerAssistant_shouldCreateAssistantMemberAndIssueTokens() {
        RegisterMemberRequest assistantRequest = new RegisterMemberRequest(
                "Assistant@classhub.com",
                "Classhub!1",
                "Assistant Lee",
                "010-4444-5555"
        );
        given(memberRepository.findByEmail("assistant@classhub.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("Classhub!1")).willReturn("encoded-password");
        AuthTokens tokens = new AuthTokens(
                UUID.randomUUID(),
                "assistant-access",
                LocalDateTime.now().plusMinutes(30),
                "assistant-refresh",
                LocalDateTime.now().plusDays(7)
        );
        given(authService.login(any(LoginRequest.class))).willReturn(tokens);

        AuthTokens result = registerService.registerAssistant(assistantRequest);

        assertThat(result).isEqualTo(tokens);
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.ASSISTANT);
        assertThat(savedMember.getEmail()).isEqualTo("assistant@classhub.com");
    }

    @Test
    void registerStudent_shouldCreateMemberAndStudentInfo() {
        given(memberRepository.findByEmail("student@classhub.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("Classhub!1")).willReturn("encoded-password");
        UUID generatedMemberId = UUID.randomUUID();
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", generatedMemberId);
            return member;
        });
        AuthTokens tokens = new AuthTokens(
                UUID.randomUUID(),
                "student-access",
                LocalDateTime.now().plusMinutes(30),
                "student-refresh",
                LocalDateTime.now().plusDays(7)
        );
        given(authService.login(any(LoginRequest.class))).willReturn(tokens);

        AuthTokens result = registerService.registerStudent(studentRequest);

        assertThat(result).isEqualTo(tokens);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.STUDENT);
        assertThat(savedMember.getName()).isEqualTo("Student Hong");

        ArgumentCaptor<StudentInfo> infoCaptor = ArgumentCaptor.forClass(StudentInfo.class);
        verify(studentInfoRepository).save(infoCaptor.capture());
        StudentInfo savedInfo = infoCaptor.getValue();
        assertThat(savedInfo.getMemberId()).isEqualTo(generatedMemberId);
        assertThat(savedInfo.getSchoolName()).isEqualTo("서울중학교");
        assertThat(savedInfo.getParentPhone()).isEqualTo("010-9999-8888");
    }

    @Test
    void registerStudent_shouldFail_whenEmailDuplicate() {
        Member existing = Member.builder()
                .email("student@classhub.com")
                .password("encoded")
                .name("Student")
                .phoneNumber("010-1234-0000")
                .role(MemberRole.STUDENT)
                .build();
        given(memberRepository.findByEmail("student@classhub.com")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> registerService.registerStudent(studentRequest))
                .isInstanceOf(BusinessException.class);
    }
}
