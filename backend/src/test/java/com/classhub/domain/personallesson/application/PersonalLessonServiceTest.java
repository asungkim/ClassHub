package com.classhub.domain.personallesson.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.personallesson.dto.request.PersonalLessonCreateRequest;
import com.classhub.domain.personallesson.dto.request.PersonalLessonUpdateRequest;
import com.classhub.domain.personallesson.dto.response.PersonalLessonResponse;
import com.classhub.domain.personallesson.dto.response.PersonalLessonSummary;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.studentprofile.application.StudentProfileService;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PersonalLessonServiceTest {

    @Autowired
    private PersonalLessonService personalLessonService;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private PersonalLessonRepository personalLessonRepository;

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
    private StudentProfileResponse studentProfile;

    @BeforeEach
    void setUp() {
        personalLessonRepository.deleteAll();
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
                        .teacherId(teacher.getId())
                        .name("Test Course")
                        .company("Test Company")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now())
                        .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                        .build()
        );

        studentProfile = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        course.getId(),
                        "Jane Doe",
                        "010-5555-0001",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        );
    }

    @Test
    @DisplayName("Teacher는 PersonalLesson을 생성하고 조회할 수 있다")
    void createAndListPersonalLessons() {
        personalLessonService.createLesson(
                teacher.getId(),
                new PersonalLessonCreateRequest(
                        studentProfile.id(),
                        LocalDate.of(2025, 1, 1),
                        "학습 내용"
                )
        );

        Page<PersonalLessonSummary> page = personalLessonService.getLessons(
                teacher.getId(),
                studentProfile.id(),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().content()).isEqualTo("학습 내용");
    }

    @Test
    @DisplayName("PersonalLesson을 수정할 수 있다")
    void updatePersonalLesson() {
        PersonalLessonResponse lesson = personalLessonService.createLesson(
                teacher.getId(),
                new PersonalLessonCreateRequest(
                        studentProfile.id(),
                        LocalDate.now(),
                        "원본 내용"
                )
        );

        PersonalLessonResponse updated = personalLessonService.updateLesson(
                teacher.getId(),
                lesson.id(),
                new PersonalLessonUpdateRequest(LocalDate.now().plusDays(1), "수정된 내용")
        );

        assertThat(updated.content()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("PersonalLesson을 삭제할 수 있다")
    void deletePersonalLesson() {
        PersonalLessonResponse lesson = personalLessonService.createLesson(
                teacher.getId(),
                new PersonalLessonCreateRequest(
                        studentProfile.id(),
                        LocalDate.now(),
                        "원본 내용"
                )
        );

        personalLessonService.deleteLesson(teacher.getId(), lesson.id());

        assertThat(personalLessonRepository.findById(lesson.id())).isEmpty();
    }
}
