package com.classhub.domain.studentcourse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class StudentCourseEnrollmentRepositoryTest {

    @Autowired
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private com.classhub.domain.member.repository.MemberRepository memberRepository;

    @Test
    void searchStudentSummariesForTeacher_shouldReturnDistinctStudents() {
        Member teacher = memberRepository.save(createMember("teacher@classhub.dev", "Teacher", MemberRole.TEACHER));
        Member studentA = memberRepository.save(createMember("a@classhub.dev", "Alice", MemberRole.STUDENT));
        Member studentB = memberRepository.save(createMember("b@classhub.dev", "Bob", MemberRole.STUDENT));

        Course course1 = courseRepository.save(createCourse(teacher.getId(), "수학 A"));
        Course course2 = courseRepository.save(createCourse(teacher.getId(), "수학 B"));

        enrollmentRepository.save(StudentCourseEnrollment.create(studentA.getId(), course1.getId(), null));
        enrollmentRepository.save(StudentCourseEnrollment.create(studentA.getId(), course2.getId(), null));
        enrollmentRepository.save(StudentCourseEnrollment.create(studentB.getId(), course1.getId(), null));

        Page<StudentStatusProjection> page = enrollmentRepository.searchStudentSummariesForTeacher(
                teacher.getId(),
                false,
                false,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(2);
        List<UUID> studentIds = page.getContent().stream()
                .map(StudentStatusProjection::getStudentMemberId)
                .toList();
        assertThat(studentIds).containsExactly(studentA.getId(), studentB.getId());
        assertThat(page.getContent()).allSatisfy(projection -> assertThat(projection.getActive()).isTrue());
    }

    @Test
    void searchStudentSummariesForTeacher_shouldFilterByActiveStatus() {
        Member teacher = memberRepository.save(createMember("teacher2@classhub.dev", "Teacher", MemberRole.TEACHER));
        Member studentActive = memberRepository.save(createMember("c@classhub.dev", "Chris", MemberRole.STUDENT));
        Member studentInactive = memberRepository.save(createMember("d@classhub.dev", "Dora", MemberRole.STUDENT));

        Course activeCourse = courseRepository.save(createCourse(teacher.getId(), "활성 반"));
        Course inactiveCourse = courseRepository.save(createCourse(teacher.getId(), "비활성 반"));
        inactiveCourse.deactivate();
        courseRepository.save(inactiveCourse);

        enrollmentRepository.save(StudentCourseEnrollment.create(studentActive.getId(), activeCourse.getId(), null));
        enrollmentRepository.save(StudentCourseEnrollment.create(studentActive.getId(), inactiveCourse.getId(), null));
        enrollmentRepository.save(StudentCourseEnrollment.create(studentInactive.getId(), inactiveCourse.getId(), null));

        Page<StudentStatusProjection> activePage = enrollmentRepository.searchStudentSummariesForTeacher(
                teacher.getId(),
                true,
                false,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(activePage.getContent()).hasSize(1);
        assertThat(activePage.getContent().getFirst().getStudentMemberId()).isEqualTo(studentActive.getId());
        assertThat(activePage.getContent().getFirst().getActive()).isTrue();

        Page<StudentStatusProjection> inactivePage = enrollmentRepository.searchStudentSummariesForTeacher(
                teacher.getId(),
                false,
                true,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(inactivePage.getContent()).hasSize(1);
        assertThat(inactivePage.getContent().getFirst().getStudentMemberId()).isEqualTo(studentInactive.getId());
        assertThat(inactivePage.getContent().getFirst().getActive()).isFalse();
    }

    @Test
    void searchStudentSummariesForTeachers_shouldFilterByTeacherIds() {
        Member teacherA = memberRepository.save(createMember("teacherA@classhub.dev", "TeacherA", MemberRole.TEACHER));
        Member teacherB = memberRepository.save(createMember("teacherB@classhub.dev", "TeacherB", MemberRole.TEACHER));
        Member studentA = memberRepository.save(createMember("e@classhub.dev", "Eve", MemberRole.STUDENT));
        Member studentB = memberRepository.save(createMember("f@classhub.dev", "Finn", MemberRole.STUDENT));

        Course courseA = courseRepository.save(createCourse(teacherA.getId(), "A 반"));
        Course courseB = courseRepository.save(createCourse(teacherB.getId(), "B 반"));

        enrollmentRepository.save(StudentCourseEnrollment.create(studentA.getId(), courseA.getId(), null));
        enrollmentRepository.save(StudentCourseEnrollment.create(studentB.getId(), courseB.getId(), null));

        Page<StudentStatusProjection> page = enrollmentRepository.searchStudentSummariesForTeachers(
                List.of(teacherA.getId()),
                false,
                false,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getStudentMemberId()).isEqualTo(studentA.getId());
    }

    private Member createMember(String email, String name, MemberRole role) {
        return Member.builder()
                .email(email)
                .name(name)
                .password("encoded")
                .phoneNumber("010-0000-0000")
                .role(role)
                .build();
    }

    private Course createCourse(UUID teacherId, String name) {
        return Course.create(
                UUID.randomUUID(),
                teacherId,
                name,
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of()
        );
    }
}
