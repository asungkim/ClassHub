package com.classhub.domain.progress.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.support.ProgressPermissionValidator.ProgressAccessMode;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProgressPermissionValidatorTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Mock
    private TeacherAssistantAssignmentRepository assistantAssignmentRepository;

    @InjectMocks
    private ProgressPermissionValidator validator;

    private UUID teacherId;
    private UUID assistantId;
    private UUID studentId;
    private MemberPrincipal teacherPrincipal;
    private MemberPrincipal assistantPrincipal;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        teacherPrincipal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        assistantPrincipal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
    }

    @Test
    void ensureCourseAccess_shouldReturnCourse_whenTeacherOwnsCourse() {
        UUID courseId = UUID.randomUUID();
        Course course = createCourse(courseId, teacherId);
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        Course result = validator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.WRITE);

        assertThat(result).isEqualTo(course);
    }

    @Test
    void ensureCourseAccess_shouldThrow_whenTeacherDoesNotOwnCourse() {
        UUID courseId = UUID.randomUUID();
        UUID otherTeacher = UUID.randomUUID();
        Course course = createCourse(courseId, otherTeacher);
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> validator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.READ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COURSE_FORBIDDEN);
    }

    @Test
    void ensureCourseAccess_shouldAllowAssistantRead_whenAssignmentActive() {
        UUID courseId = UUID.randomUUID();
        Course course = createCourse(courseId, teacherId);
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(assistantAssignmentRepository.findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .willReturn(Optional.of(TeacherAssistantAssignment.create(teacherId, assistantId)));

        Course result = validator.ensureCourseAccess(assistantPrincipal, courseId, ProgressAccessMode.READ);

        assertThat(result).isEqualTo(course);
    }

    @Test
    void ensureCourseAccess_shouldThrow_whenAssistantAssignmentMissing() {
        UUID courseId = UUID.randomUUID();
        Course course = createCourse(courseId, teacherId);
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(assistantAssignmentRepository.findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.ensureCourseAccess(assistantPrincipal, courseId, ProgressAccessMode.READ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureCourseAccess_shouldRejectAssistantWrite() {
        UUID courseId = UUID.randomUUID();

        assertThatThrownBy(() -> validator.ensureCourseAccess(assistantPrincipal, courseId, ProgressAccessMode.WRITE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureCourseAccess_shouldRejectStudent() {
        UUID courseId = UUID.randomUUID();
        MemberPrincipal studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);

        assertThatThrownBy(() -> validator.ensureCourseAccess(studentPrincipal, courseId, ProgressAccessMode.READ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureRecordAccess_shouldReturnRecord_whenTeacherOwnsCourse() {
        UUID recordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(recordId, studentId, courseId);
        Course course = createCourse(courseId, teacherId);
        given(studentCourseRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        StudentCourseRecord result = validator.ensureRecordAccess(teacherPrincipal, recordId, ProgressAccessMode.WRITE);

        assertThat(result).isEqualTo(record);
    }

    @Test
    void ensureRecordAccess_shouldThrow_whenRecordMissing() {
        UUID recordId = UUID.randomUUID();
        given(studentCourseRecordRepository.findById(recordId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.ensureRecordAccess(teacherPrincipal, recordId, ProgressAccessMode.READ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.STUDENT_COURSE_RECORD_NOT_FOUND);
    }

    @Test
    void ensureRecordAccess_shouldAllowAssistantRead_whenAssignmentActive() {
        UUID recordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(recordId, studentId, courseId);
        Course course = createCourse(courseId, teacherId);
        given(studentCourseRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(assistantAssignmentRepository.findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .willReturn(Optional.of(TeacherAssistantAssignment.create(teacherId, assistantId)));

        StudentCourseRecord result = validator.ensureRecordAccess(assistantPrincipal, recordId, ProgressAccessMode.READ);

        assertThat(result).isEqualTo(record);
    }

    @Test
    void ensureRecordAccess_shouldRejectAssistantWrite() {
        UUID recordId = UUID.randomUUID();

        assertThatThrownBy(() -> validator.ensureRecordAccess(assistantPrincipal, recordId, ProgressAccessMode.WRITE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureCalendarAccess_shouldReturnRecordsForTeacher() {
        List<StudentCourseRecord> records = List.of(createRecord(UUID.randomUUID(), studentId, UUID.randomUUID()));
        given(studentCourseRecordRepository.findActiveByStudentIdAndTeacherId(studentId, teacherId))
                .willReturn(records);

        List<StudentCourseRecord> result = validator.ensureCalendarAccess(teacherPrincipal, studentId);

        assertThat(result).isEqualTo(records);
    }

    @Test
    void ensureCalendarAccess_shouldThrow_whenTeacherHasNoRecords() {
        given(studentCourseRecordRepository.findActiveByStudentIdAndTeacherId(studentId, teacherId))
                .willReturn(List.of());

        assertThatThrownBy(() -> validator.ensureCalendarAccess(teacherPrincipal, studentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureCalendarAccess_shouldReturnRecordsForAssistant() {
        UUID teacherB = UUID.randomUUID();
        List<StudentCourseRecord> records = List.of(createRecord(UUID.randomUUID(), studentId, UUID.randomUUID()));
        given(assistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .willReturn(List.of(
                        TeacherAssistantAssignment.create(teacherId, assistantId),
                        TeacherAssistantAssignment.create(teacherB, assistantId)
                ));
        given(studentCourseRecordRepository.findActiveByStudentIdAndTeacherIds(studentId, List.of(teacherId, teacherB)))
                .willReturn(records);

        List<StudentCourseRecord> result = validator.ensureCalendarAccess(assistantPrincipal, studentId);

        assertThat(result).isEqualTo(records);
    }

    @Test
    void ensureCalendarAccess_shouldThrow_whenAssistantHasNoAssignment() {
        given(assistantAssignmentRepository.findByAssistantMemberIdAndDeletedAtIsNull(assistantId))
                .willReturn(List.of());

        assertThatThrownBy(() -> validator.ensureCalendarAccess(assistantPrincipal, studentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureCalendarAccess_shouldThrow_forStudentRole() {
        MemberPrincipal studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);

        assertThatThrownBy(() -> validator.ensureCalendarAccess(studentPrincipal, studentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    private Course createCourse(UUID courseId, UUID ownerId) {
        Course course = Course.create(
                UUID.randomUUID(),
                ownerId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        ReflectionTestUtils.setField(course, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(course, "updatedAt", LocalDateTime.now());
        return course;
    }

    private StudentCourseRecord createRecord(UUID recordId, UUID student, UUID courseId) {
        StudentCourseRecord record = StudentCourseRecord.create(student, courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", recordId);
        ReflectionTestUtils.setField(record, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(record, "updatedAt", LocalDateTime.now());
        return record;
    }
}
