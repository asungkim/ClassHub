package com.classhub.domain.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.member.dto.request.MemberProfileUpdateRequest;
import com.classhub.domain.member.dto.request.StudentInfoUpdateRequest;
import com.classhub.domain.member.dto.response.MemberProfileResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StudentInfoRepository studentInfoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberProfileService memberProfileService;

    private UUID memberId;
    private Member teacher;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        teacher = Member.builder()
                .email("teacher@classhub.dev")
                .password("encoded")
                .name("Teacher")
                .phoneNumber("010-1111-2222")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(teacher, "id", memberId);
    }

    @Test
    void getProfile_shouldReturnMemberOnly_whenTeacher() {
        given(memberRepository.findById(memberId)).willReturn(Optional.of(teacher));

        MemberProfileResponse response = memberProfileService.getProfile(memberId);

        assertThat(response.member().memberId()).isEqualTo(memberId);
        assertThat(response.member().email()).isEqualTo("teacher@classhub.dev");
        assertThat(response.member().phoneNumber()).isEqualTo("010-1111-2222");
        assertThat(response.studentInfo()).isNull();
        verify(studentInfoRepository, never()).findByMemberId(memberId);
    }

    @Test
    void getProfile_shouldReturnMemberAndStudentInfo_whenStudent() {
        Member student = Member.builder()
                .email("student@classhub.dev")
                .password("encoded")
                .name("Student")
                .phoneNumber("010-3333-4444")
                .role(MemberRole.STUDENT)
                .build();
        ReflectionTestUtils.setField(student, "id", memberId);
        StudentInfo info = StudentInfo.builder()
                .memberId(memberId)
                .schoolName("ClassHub High")
                .grade(StudentGrade.HIGH_2)
                .birthDate(LocalDate.of(2008, 3, 1))
                .parentPhone("010-9999-0000")
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(student));
        given(studentInfoRepository.findByMemberId(memberId)).willReturn(Optional.of(info));

        MemberProfileResponse response = memberProfileService.getProfile(memberId);

        assertThat(response.member().role()).isEqualTo(MemberRole.STUDENT);
        assertThat(response.studentInfo()).isNotNull();
        assertThat(response.studentInfo().schoolName()).isEqualTo("ClassHub High");
        assertThat(response.studentInfo().grade()).isEqualTo(StudentGrade.HIGH_2);
    }

    @Test
    void updateProfile_shouldUpdateMemberFields_whenValidRequest() {
        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
                "NewTeacher@classhub.dev",
                "New Name",
                "01012345678",
                null,
                null
        );
        given(memberRepository.findById(memberId)).willReturn(Optional.of(teacher));
        given(memberRepository.findByEmail("newteacher@classhub.dev")).willReturn(Optional.empty());

        MemberProfileResponse response = memberProfileService.updateProfile(memberId, request);

        assertThat(response.member().email()).isEqualTo("newteacher@classhub.dev");
        assertThat(response.member().name()).isEqualTo("New Name");
        assertThat(response.member().phoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    void updateProfile_shouldEncodePassword_whenPasswordProvided() {
        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
                null,
                null,
                null,
                "NewPass!1",
                null
        );
        given(memberRepository.findById(memberId)).willReturn(Optional.of(teacher));
        given(passwordEncoder.encode("NewPass!1")).willReturn("encoded-new");

        memberProfileService.updateProfile(memberId, request);

        assertThat(teacher.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void updateProfile_shouldRejectDuplicateEmail_whenEmailChanges() {
        Member existing = Member.builder()
                .email("newteacher@classhub.dev")
                .password("encoded")
                .name("Other")
                .phoneNumber("010-0000-0000")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());

        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
                "newteacher@classhub.dev",
                null,
                null,
                null,
                null
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.of(teacher));
        given(memberRepository.findByEmail("newteacher@classhub.dev")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> memberProfileService.updateProfile(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.DUPLICATE_EMAIL);
    }

    @Test
    void updateProfile_shouldUpdateStudentInfo_whenStudent() {
        Member student = Member.builder()
                .email("student@classhub.dev")
                .password("encoded")
                .name("Student")
                .phoneNumber("010-1111-2222")
                .role(MemberRole.STUDENT)
                .build();
        ReflectionTestUtils.setField(student, "id", memberId);
        StudentInfo info = StudentInfo.builder()
                .memberId(memberId)
                .schoolName("Old School")
                .grade(StudentGrade.HIGH_1)
                .birthDate(LocalDate.of(2009, 1, 1))
                .parentPhone("010-9999-1111")
                .build();

        StudentInfoUpdateRequest studentInfoUpdate = new StudentInfoUpdateRequest(
                "New School",
                StudentGrade.HIGH_2,
                LocalDate.of(2008, 2, 2),
                "01022223333"
        );
        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
                null,
                null,
                null,
                null,
                studentInfoUpdate
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.of(student));
        given(studentInfoRepository.findByMemberId(memberId)).willReturn(Optional.of(info));

        MemberProfileResponse response = memberProfileService.updateProfile(memberId, request);

        assertThat(response.studentInfo().schoolName()).isEqualTo("New School");
        assertThat(response.studentInfo().grade()).isEqualTo(StudentGrade.HIGH_2);
        assertThat(response.studentInfo().parentPhone()).isEqualTo("010-2222-3333");
    }
}
