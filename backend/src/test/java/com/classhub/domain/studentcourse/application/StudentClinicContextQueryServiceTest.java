package com.classhub.domain.studentcourse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentcourse.dto.response.StudentClinicContextResponse;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentClinicContextQueryServiceTest {

    @Mock
    private StudentCourseRecordRepository recordRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private StudentClinicContextQueryService queryService;

    private UUID studentId;
    private UUID recordId;
    private UUID courseId;
    private UUID branchId;
    private UUID companyId;
    private UUID teacherId;
    private UUID defaultSlotId;
    private StudentCourseRecord record;
    private Course course;
    private CourseResponse courseResponse;
    private Member teacher;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        recordId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        defaultSlotId = UUID.randomUUID();

        record = StudentCourseRecord.create(studentId, courseId, null, defaultSlotId, null);
        ReflectionTestUtils.setField(record, "id", recordId);

        course = Course.create(
                branchId,
                teacherId,
                "고2 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);

        courseResponse = new CourseResponse(
                courseId,
                branchId,
                "강남",
                companyId,
                "러셀",
                "고2 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                true,
                List.of()
        );

        teacher = Member.builder()
                .email("teacher@classhub.io")
                .password("pw")
                .name("Teacher")
                .phoneNumber("01000000000")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(teacher, "id", teacherId);
    }

    @Test
    void getContexts_shouldReturnMappedContexts() {
        Branch branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.VERIFIED);
        ReflectionTestUtils.setField(branch, "id", branchId);
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", companyId);
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                Map.of(branchId, branch),
                Map.of(companyId, company),
                Map.of(branchId, companyId)
        );

        when(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .thenReturn(List.of(record));
        when(courseRepository.findAllById(anyCollection()))
                .thenReturn(List.of(course));
        when(courseViewAssembler.buildContext(anyCollection()))
                .thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context))
                .thenReturn(courseResponse);
        when(memberRepository.findAllById(anyCollection()))
                .thenReturn(List.of(teacher));

        List<StudentClinicContextResponse> responses = queryService.getContexts(studentId);

        assertThat(responses).hasSize(1);
        StudentClinicContextResponse response = responses.get(0);
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.courseName()).isEqualTo("고2 수학");
        assertThat(response.recordId()).isEqualTo(recordId);
        assertThat(response.defaultClinicSlotId()).isEqualTo(defaultSlotId);
        assertThat(response.teacherId()).isEqualTo(teacherId);
        assertThat(response.teacherName()).isEqualTo("Teacher");
        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.branchName()).isEqualTo("강남");
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.companyName()).isEqualTo("러셀");

        verify(recordRepository).findByStudentMemberIdAndDeletedAtIsNull(studentId);
    }
}
