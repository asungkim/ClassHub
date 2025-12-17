package com.classhub.domain.studentprofile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.dto.response.EnrolledCourseInfo;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.dto.response.StudentProfileSummary;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member otherTeacher;
    private Member assistant;
    private Course courseA;
    private Course courseB;
    private Course otherTeacherCourse;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
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

        otherTeacher = memberRepository.save(
                Member.builder()
                        .email("other_teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Lee")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        courseA = createCourse(teacher.getId(), "Course A");
        courseB = createCourse(teacher.getId(), "Course B");
        otherTeacherCourse = createCourse(otherTeacher.getId(), "Other Teacher Course");
    }

    @Test
    @DisplayName("학생 프로필 생성 시 여러 Course에 동시에 등록된다")
    void createProfile_withMultipleCourses() {
        StudentProfileResponse response = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(courseA.getId(), courseB.getId()),
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        );

        assertThat(response.enrolledCourses()).hasSize(2);
        List<StudentCourseEnrollment> enrollments =
                enrollmentRepository.findAllByStudentProfileId(response.id());
        assertThat(enrollments).extracting(StudentCourseEnrollment::getCourseId)
                .containsExactlyInAnyOrder(courseA.getId(), courseB.getId());
    }

    @Test
    @DisplayName("담당 조교 없이도 학생 프로필을 생성할 수 있다")
    void createProfile_withoutAssistant() {
        StudentProfileResponse response = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(courseA.getId()),
                        "No Assistant",
                        "010-1111-0001",
                        null,
                        "01012345678",
                        "Seoul High",
                        "1",
                        14,
                        null
                )
        );

        assertThat(response.assistantId()).isNull();
    }

    @Test
    @DisplayName("동일 Teacher 내에서 전화번호가 중복되면 등록이 실패한다")
    void createProfile_duplicatePhoneNumber() {
        studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(courseA.getId()),
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        );

        assertThatThrownBy(() -> studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(courseB.getId()),
                        "John Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345679",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Teacher는 소유하지 않은 Course로 수강 등록할 수 없다")
    void createProfile_courseForbidden() {
        assertThatThrownBy(() -> studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(otherTeacherCourse.getId()),
                        "Jane Doe",
                        "010-9999-0002",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Course ID가 중복되면 예외가 발생한다")
    void createProfile_duplicateCourseIds() {
        assertThatThrownBy(() -> studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(courseA.getId(), courseA.getId()),
                        "Jane Doe",
                        "010-9999-0003",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Course 목록을 수정하면 Enrollment가 추가/삭제된다")
    void updateProfile_syncEnrollments() {
        StudentProfileResponse created = createDefaultProfile(List.of(courseA.getId()));

        StudentProfileResponse updated = studentProfileService.updateProfile(
                teacher.getId(),
                created.id(),
                new StudentProfileUpdateRequest(
                        "Jane Updated",
                        "01098765432",
                        "Seoul High",
                        "2",
                        List.of(courseA.getId(), courseB.getId()),
                        assistant.getId(),
                        "010-0000-2222",
                        null,
                        null,
                        16
                )
        );

        assertThat(updated.enrolledCourses()).hasSize(2);
        List<StudentCourseEnrollment> enrollments =
                enrollmentRepository.findAllByStudentProfileId(created.id());
        assertThat(enrollments).extracting(StudentCourseEnrollment::getCourseId)
                .containsExactlyInAnyOrder(courseA.getId(), courseB.getId());
    }

    @Test
    @DisplayName("CourseIds에 빈 리스트를 전달하면 모든 수강 정보가 제거된다")
    void updateProfile_removeAllCourses() {
        StudentProfileResponse created = createDefaultProfile(List.of(courseA.getId(), courseB.getId()));

        StudentProfileResponse updated = studentProfileService.updateProfile(
                teacher.getId(),
                created.id(),
                new StudentProfileUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThat(updated.enrolledCourses()).isEmpty();
        assertThat(enrollmentRepository.findAllByStudentProfileId(created.id())).isEmpty();
    }

    @Test
    @DisplayName("courseIds가 null이면 수강 정보가 유지된다")
    void updateProfile_noCourseChangeWhenNull() {
        StudentProfileResponse created = createDefaultProfile(List.of(courseA.getId()));

        studentProfileService.updateProfile(
                teacher.getId(),
                created.id(),
                new StudentProfileUpdateRequest(
                        "Jane Updated",
                        null,
                        null,
                        null,
                        null,
                        assistant.getId(),
                        null,
                        null,
                        null,
                        null
                )
        );

        List<StudentCourseEnrollment> enrollments =
                enrollmentRepository.findAllByStudentProfileId(created.id());
        assertThat(enrollments).extracting(StudentCourseEnrollment::getCourseId)
                .containsExactly(courseA.getId());
    }

    @Test
    @DisplayName("프로필 조회 시 수강 중인 Course 목록이 반환된다")
    void getProfile_returnsEnrolledCourses() {
        StudentProfileResponse created = createDefaultProfile(List.of(courseA.getId(), courseB.getId()));

        StudentProfileResponse response = studentProfileService.getProfile(
                teacher.getId(),
                created.id()
        );

        assertThat(response.enrolledCourses())
                .extracting(EnrolledCourseInfo::courseId)
                .containsExactlyInAnyOrder(courseA.getId(), courseB.getId());
    }

    @Test
    @DisplayName("Teacher는 학생 프로필을 비활성화할 수 있다")
    void deleteProfile_success() {
        StudentProfileResponse created = createDefaultProfile(List.of(courseA.getId()));

        studentProfileService.deleteProfile(teacher.getId(), created.id());

        StudentProfile profile = studentProfileRepository.findById(created.id()).orElseThrow();
        assertThat(profile.isActive()).isFalse();
    }

    @Test
    @DisplayName("Course별 학생 목록을 조회할 수 있다")
    void getCourseStudents_success() {
        StudentProfileResponse created = createDefaultProfile(List.of(courseA.getId(), courseB.getId()));

        List<StudentProfileSummary> summaries = studentProfileService.getCourseStudents(
                teacher.getId(),
                courseB.getId()
        );

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().id()).isEqualTo(created.id());
        assertThat(summaries.getFirst().courseNames()).contains(courseB.getName());
    }

    private Course createCourse(UUID teacherId, String name) {
        return courseRepository.save(
                Course.builder()
                        .teacherId(teacherId)
                        .name(name)
                        .company("Test Company")
                        .schedules(new HashSet<>(Set.of(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))
                        )))
                        .build()
        );
    }

    private StudentProfileResponse createDefaultProfile(List<UUID> courseIds) {
        return studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        courseIds,
                        "Jane Doe",
                        "010-9999-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        );
    }
}
