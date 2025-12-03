package com.classhub.domain.studentprofile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StudentProfileServiceTest {

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member assistant;
    private Course course;

    @BeforeEach
    void setUp() {
        studentProfileRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Kim")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant Park")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );

        course = courseRepository.save(
                Course.builder()
                        .name("Math 101")
                        .company("ClassHub")
                        .teacherId(teacher.getId())
                        .build()
        );
    }

    @Test
    @DisplayName("Teacher는 학생 프로필을 생성할 수 있다")
    void createProfile_success() {
        StudentProfileResponse response = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        course.getId(),
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null,
                        null
                )
        );

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.teacherId()).isEqualTo(teacher.getId());
        assertThat(response.phoneNumber()).isEqualTo("010-9999-0001");
    }

    @Test
    @DisplayName("학생번호가 중복되면 예외가 발생한다")
    void createProfile_duplicateStudentNumber() {
        studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        course.getId(),
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null,
                        null
                )
        );

        assertThatThrownBy(() -> studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        course.getId(),
                        "John Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345679",
                        "Seoul High",
                        "1",
                        15,
                        null,
                        null
                )
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Teacher는 담당 조교와 학번을 수정할 수 있다")
    void updateProfile_success() {
        StudentProfileResponse created = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        course.getId(),
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null,
                        null
                )
        );

        Member newAssistant = memberRepository.save(
                Member.builder()
                        .email("assistant2@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant Lee")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );

        StudentProfileResponse updated = studentProfileService.updateProfile(
                teacher.getId(),
                created.id(),
                new StudentProfileUpdateRequest(
                        "Jane Updated",
                        "01098765432",
                        "Seoul High",
                        "2",
                        newAssistant.getId(),
                        "010-0000-2222",
                        null,
                        null,
                        16
                )
        );

        assertThat(updated.assistantId()).isEqualTo(newAssistant.getId());
        assertThat(updated.phoneNumber()).isEqualTo("010-0000-2222");
        assertThat(updated.parentPhone()).isEqualTo("01098765432");
    }

    @Test
    @DisplayName("Teacher는 학생 프로필을 비활성화할 수 있다")
    void deleteProfile_success() {
        StudentProfileResponse created = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        course.getId(),
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null,
                        null
                )
        );

        studentProfileService.deleteProfile(teacher.getId(), created.id());

        StudentProfile profile = studentProfileRepository.findById(created.id()).orElseThrow();
        assertThat(profile.isActive()).isFalse();
    }
}
