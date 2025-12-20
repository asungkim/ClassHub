package com.classhub.domain.studentcourse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.dto.StudentCourseStatusFilter;
import com.classhub.domain.studentcourse.dto.request.StudentCourseRecordUpdateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse;
import com.classhub.domain.studentcourse.dto.response.StudentCourseListItemResponse;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class StudentCourseManagementServiceTest {

    @Mock
    private StudentCourseRecordRepository recordRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StudentInfoRepository studentInfoRepository;

    @Mock
    private TeacherAssistantAssignmentRepository assignmentRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private StudentCourseManagementService managementService;

    private UUID teacherId;
    private UUID assistantId;
    private UUID studentId;
    private UUID courseId;
    private StudentCourseRecord record;
    private Course course;
    private Member studentMember;
    private StudentInfo studentInfo;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        record = StudentCourseRecord.create(studentId, courseId, null, null, "메모");
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        course = Course.create(
                UUID.randomUUID(),
                teacherId,
                "고2 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                java.util.Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        studentMember = Member.builder()
                .role(MemberRole.STUDENT)
                .email("student@classhub.dev")
                .name("홍길동")
                .phoneNumber("010-0000-0000")
                .password("encoded")
                .build();
        ReflectionTestUtils.setField(studentMember, "id", studentId);
        studentInfo = StudentInfo.builder()
                .memberId(studentId)
                .schoolName("ClassHub 고등학교")
                .grade(StudentGrade.HIGH_2)
                .birthDate(LocalDate.now().minusYears(18))
                .parentPhone("010-1111-2222")
                .build();
        courseResponse = new CourseResponse(
                courseId,
                UUID.randomUUID(),
                "잠실 B102",
                UUID.randomUUID(),
                "러셀",
                "고2 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
    }

    @Test
    void shouldListStudentsForTeacher_withFilters() {
        Page<StudentCourseRecord> page = new PageImpl<>(List.of(record), PageRequest.of(0, 10), 1);
        when(recordRepository.searchRecordsForTeacher(
                eq(teacherId),
                eq(courseId),
                eq(true),
                eq(false),
                eq("hong"),
                eq(PageRequest.of(0, 10))
        )).thenReturn(page);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(studentMember));
        when(studentInfoRepository.findByMemberIdIn(anyCollection())).thenReturn(List.of(studentInfo));
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));

        PageResponse<StudentCourseListItemResponse> response = managementService.getStudentCourses(
                new MemberPrincipal(teacherId, MemberRole.TEACHER),
                courseId,
                StudentCourseStatusFilter.ACTIVE,
                "hong",
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        StudentCourseListItemResponse item = response.content().getFirst();
        assertThat(item.studentName()).isEqualTo("홍길동");
        assertThat(item.courseName()).isEqualTo("고2 수학");
        assertThat(item.parentPhoneNumber()).isEqualTo("010-1111-2222");
        verify(recordRepository).searchRecordsForTeacher(
                teacherId,
                courseId,
                true,
                false,
                "hong",
                PageRequest.of(0, 10)
        );
    }

    @Test
    void shouldListStudentsForAssistant_whenAssignmentsExist() {
        Page<StudentCourseRecord> page = new PageImpl<>(List.of(record), PageRequest.of(0, 5), 1);
        when(assignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of(TeacherAssistantAssignment.create(teacherId, assistantId)));
        when(recordRepository.searchRecordsForTeachers(
                eq(List.of(teacherId)),
                eq(null),
                eq(false),
                eq(false),
                eq(null),
                eq(PageRequest.of(0, 5))
        ))
                .thenReturn(page);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(studentMember));
        when(studentInfoRepository.findByMemberIdIn(anyCollection())).thenReturn(List.of(studentInfo));
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));

        PageResponse<StudentCourseListItemResponse> response = managementService.getStudentCourses(
                new MemberPrincipal(assistantId, MemberRole.ASSISTANT),
                null,
                StudentCourseStatusFilter.ALL,
                null,
                0,
                5
        );

        assertThat(response.content()).hasSize(1);
        verify(recordRepository).searchRecordsForTeachers(
                eq(List.of(teacherId)),
                eq(null),
                eq(false),
                eq(false),
                eq(null),
                eq(PageRequest.of(0, 5))
        );
    }

    @Test
    void shouldReturnEmptyPageForAssistantWithoutAssignments() {
        when(assignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .thenReturn(List.of());

        PageResponse<StudentCourseListItemResponse> response = managementService.getStudentCourses(
                new MemberPrincipal(assistantId, MemberRole.ASSISTANT),
                null,
                StudentCourseStatusFilter.ACTIVE,
                null,
                0,
                10
        );

        assertThat(response.content()).isEmpty();
        verify(recordRepository, never()).searchRecordsForTeachers(
                anyList(),
                any(),
                anyBoolean(),
                anyBoolean(),
                any(),
                any()
        );
    }

    @Test
    void shouldGetDetailForTeacher() {
        when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(memberRepository.findById(studentId)).thenReturn(Optional.of(studentMember));
        when(studentInfoRepository.findByMemberId(studentId)).thenReturn(Optional.of(studentInfo));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        StudentCourseDetailResponse response = managementService.getStudentCourseDetail(teacherId, record.getId());

        assertThat(response.student().name()).isEqualTo("홍길동");
        assertThat(response.course().name()).isEqualTo("고2 수학");
    }

    @Test
    void shouldThrowWhenAssistantRequestsDetail() {
        when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> managementService.getStudentCourseDetail(assistantId, record.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void shouldUpdateRecordForTeacher() {
        when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(assignmentRepository.findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .thenReturn(Optional.of(TeacherAssistantAssignment.create(teacherId, assistantId)));
        when(memberRepository.findById(studentId)).thenReturn(Optional.of(studentMember));
        when(studentInfoRepository.findByMemberId(studentId)).thenReturn(Optional.of(studentInfo));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        StudentCourseRecordUpdateRequest request = new StudentCourseRecordUpdateRequest(
                assistantId,
                UUID.randomUUID(),
                "새 메모"
        );

        StudentCourseDetailResponse response = managementService.updateStudentCourseRecord(
                teacherId,
                record.getId(),
                request
        );

        assertThat(response.teacherNotes()).isEqualTo("새 메모");
        assertThat(response.assistantMemberId()).isEqualTo(assistantId);
    }

    @Test
    void shouldValidateAssistantAssignmentOnUpdate() {
        when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(assignmentRepository.findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .thenReturn(Optional.empty());

        StudentCourseRecordUpdateRequest request = new StudentCourseRecordUpdateRequest(
                assistantId,
                null,
                null
        );

        assertThatThrownBy(() -> managementService.updateStudentCourseRecord(teacherId, record.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }
}
