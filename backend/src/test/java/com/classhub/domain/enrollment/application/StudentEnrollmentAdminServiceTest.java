package com.classhub.domain.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse.StudentSummary;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.repository.StudentEnrollmentRequestRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.response.PageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentAdminServiceTest {

    @Mock
    private StudentEnrollmentRequestRepository requestRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StudentInfoRepository studentInfoRepository;

    @InjectMocks
    private StudentEnrollmentAdminService adminService;

    private StudentEnrollmentRequest request;
    private Course course;
    private CourseResponse courseResponse;
    private Member studentMember;
    private StudentInfo studentInfo;
    private UUID teacherId;
    private UUID courseId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        request = StudentEnrollmentRequest.builder()
                .courseId(courseId)
                .studentMemberId(studentId)
                .status(EnrollmentStatus.PENDING)
                .message("참석하고 싶습니다")
                .build();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        course = Course.create(
                UUID.randomUUID(),
                teacherId,
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                java.util.Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        courseResponse = new CourseResponse(
                courseId,
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
        studentMember = Member.builder()
                .email("student@classhub.dev")
                .name("홍길동")
                .phoneNumber("010-0000-0000")
                .password("encoded")
                .role(MemberRole.STUDENT)
                .build();
        ReflectionTestUtils.setField(studentMember, "id", studentId);
        studentInfo = StudentInfo.builder()
                .memberId(studentId)
                .schoolName("ClassHub 고등학교")
                .grade(StudentGrade.HIGH_2)
                .birthDate(LocalDate.now().minusYears(18))
                .parentPhone("010-1111-2222")
                .build();
    }

    @Test
    void shouldListRequestsForAdmin_withFilters() {
        Page<StudentEnrollmentRequest> page = new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1);
        when(requestRepository.searchRequestsForAdmin(
                eq(teacherId),
                eq(courseId),
                anySet(),
                eq("hong"),
                eq(PageRequest.of(0, 10))
        )).thenReturn(page);
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(studentMember));
        when(studentInfoRepository.findByMemberIdIn(anyCollection())).thenReturn(List.of(studentInfo));

        PageResponse<TeacherEnrollmentRequestResponse> response = adminService.getRequests(
                teacherId,
                courseId,
                Set.of(EnrollmentStatus.PENDING),
                "hong",
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        TeacherEnrollmentRequestResponse dto = response.content().getFirst();
        StudentSummary summary = dto.student();
        assertThat(summary.name()).isEqualTo("홍길동");
        verify(requestRepository).searchRequestsForAdmin(
                eq(teacherId),
                eq(courseId),
                anySet(),
                eq("hong"),
                eq(PageRequest.of(0, 10))
        );
    }
}
