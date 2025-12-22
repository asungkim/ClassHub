package com.classhub.domain.clinic.record.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.record.dto.request.ClinicRecordCreateRequest;
import com.classhub.domain.clinic.record.dto.request.ClinicRecordUpdateRequest;
import com.classhub.domain.clinic.record.model.ClinicRecord;
import com.classhub.domain.clinic.record.repository.ClinicRecordRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClinicRecordServiceTest {

    @Mock
    private ClinicRecordRepository clinicRecordRepository;
    @Mock
    private ClinicAttendanceRepository clinicAttendanceRepository;
    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;

    @InjectMocks
    private ClinicRecordService clinicRecordService;

    @Test
    void createRecord_shouldSave_whenTeacherAuthorized() {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID recordStudentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicAttendance attendance = createAttendance(attendanceId, UUID.randomUUID());
        StudentCourseRecord record = createStudentCourseRecord(recordId, recordStudentId, courseId);
        Course course = createCourse(courseId, teacherId);
        ClinicRecordCreateRequest request = new ClinicRecordCreateRequest(
                attendanceId,
                "title",
                "content",
                "done"
        );
        ClinicRecord savedRecord = ClinicRecord.builder()
                .clinicAttendanceId(attendanceId)
                .writerId(teacherId)
                .title("title")
                .content("content")
                .homeworkProgress("done")
                .build();
        ReflectionTestUtils.setField(savedRecord, "id", recordId);

        given(clinicAttendanceRepository.findById(attendanceId)).willReturn(Optional.of(attendance));
        given(clinicRecordRepository.existsByClinicAttendanceId(attendanceId)).willReturn(false);
        given(studentCourseRecordRepository.findById(attendance.getStudentCourseRecordId()))
                .willReturn(Optional.of(record));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(clinicRecordRepository.save(any(ClinicRecord.class))).willReturn(savedRecord);

        ClinicRecord result = clinicRecordService.createRecord(principal, request);

        assertThat(result.getId()).isEqualTo(recordId);
        assertThat(result.getClinicAttendanceId()).isEqualTo(attendanceId);
        assertThat(result.getWriterId()).isEqualTo(teacherId);
    }

    @Test
    void createRecord_shouldThrow_whenRecordAlreadyExists() {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicRecordCreateRequest request = new ClinicRecordCreateRequest(
                attendanceId,
                "title",
                "content",
                null
        );

        given(clinicRecordRepository.existsByClinicAttendanceId(attendanceId)).willReturn(true);

        assertThatThrownBy(() -> clinicRecordService.createRecord(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_RECORD_ALREADY_EXISTS);
        verify(clinicRecordRepository, never()).save(any());
    }

    @Test
    void createRecord_shouldThrow_whenAssistantNotAssigned() {
        UUID teacherId = UUID.randomUUID();
        UUID assistantId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID recordStudentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
        ClinicAttendance attendance = createAttendance(attendanceId, UUID.randomUUID());
        StudentCourseRecord record = createStudentCourseRecord(recordId, recordStudentId, courseId);
        Course course = createCourse(courseId, teacherId);
        ClinicRecordCreateRequest request = new ClinicRecordCreateRequest(
                attendanceId,
                "title",
                "content",
                null
        );

        given(clinicAttendanceRepository.findById(attendanceId)).willReturn(Optional.of(attendance));
        given(clinicRecordRepository.existsByClinicAttendanceId(attendanceId)).willReturn(false);
        given(studentCourseRecordRepository.findById(attendance.getStudentCourseRecordId()))
                .willReturn(Optional.of(record));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(teacherAssistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> clinicRecordService.createRecord(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void getRecord_shouldThrow_whenStudentAccess() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID recordStudentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(studentId, MemberRole.STUDENT);
        ClinicRecord record = ClinicRecord.builder()
                .clinicAttendanceId(attendanceId)
                .writerId(teacherId)
                .title("title")
                .content("content")
                .homeworkProgress(null)
                .build();
        ReflectionTestUtils.setField(record, "id", recordId);
        ClinicAttendance attendance = createAttendance(attendanceId, UUID.randomUUID());
        StudentCourseRecord studentCourseRecord = createStudentCourseRecord(recordId, recordStudentId, courseId);
        Course course = createCourse(courseId, teacherId);

        given(clinicRecordRepository.findByClinicAttendanceId(attendanceId)).willReturn(Optional.of(record));
        given(clinicAttendanceRepository.findById(attendanceId)).willReturn(Optional.of(attendance));
        given(studentCourseRecordRepository.findById(attendance.getStudentCourseRecordId()))
                .willReturn(Optional.of(studentCourseRecord));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> clinicRecordService.getRecord(principal, attendanceId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void updateRecord_shouldApplyChanges_whenTeacherAuthorized() {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID studentCourseRecordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicRecord record = ClinicRecord.builder()
                .clinicAttendanceId(attendanceId)
                .writerId(teacherId)
                .title("old")
                .content("old")
                .homeworkProgress("old")
                .build();
        ReflectionTestUtils.setField(record, "id", recordId);
        ClinicAttendance attendance = createAttendance(attendanceId, studentCourseRecordId);
        StudentCourseRecord studentCourseRecord =
                createStudentCourseRecord(studentCourseRecordId, UUID.randomUUID(), courseId);
        Course course = createCourse(courseId, teacherId);
        ClinicRecordUpdateRequest request = new ClinicRecordUpdateRequest("new", "updated", "done");

        given(clinicRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(clinicAttendanceRepository.findById(attendanceId)).willReturn(Optional.of(attendance));
        given(studentCourseRecordRepository.findById(studentCourseRecordId))
                .willReturn(Optional.of(studentCourseRecord));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(clinicRecordRepository.save(any(ClinicRecord.class))).willReturn(record);

        ClinicRecord updated = clinicRecordService.updateRecord(principal, recordId, request);

        assertThat(updated.getTitle()).isEqualTo("new");
        assertThat(updated.getContent()).isEqualTo("updated");
        assertThat(updated.getHomeworkProgress()).isEqualTo("done");
    }

    @Test
    void deleteRecord_shouldDelete_whenTeacherAuthorized() {
        UUID teacherId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID studentCourseRecordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicRecord record = ClinicRecord.builder()
                .clinicAttendanceId(attendanceId)
                .writerId(teacherId)
                .title("title")
                .content("content")
                .homeworkProgress(null)
                .build();
        ReflectionTestUtils.setField(record, "id", recordId);
        ClinicAttendance attendance = createAttendance(attendanceId, studentCourseRecordId);
        StudentCourseRecord studentCourseRecord =
                createStudentCourseRecord(studentCourseRecordId, UUID.randomUUID(), courseId);
        Course course = createCourse(courseId, teacherId);

        given(clinicRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(clinicAttendanceRepository.findById(attendanceId)).willReturn(Optional.of(attendance));
        given(studentCourseRecordRepository.findById(studentCourseRecordId))
                .willReturn(Optional.of(studentCourseRecord));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        clinicRecordService.deleteRecord(principal, recordId);

        verify(clinicRecordRepository).delete(record);
    }

    private ClinicAttendance createAttendance(UUID attendanceId, UUID recordId) {
        ClinicAttendance attendance = ClinicAttendance.builder()
                .clinicSessionId(UUID.randomUUID())
                .studentCourseRecordId(recordId)
                .build();
        ReflectionTestUtils.setField(attendance, "id", attendanceId);
        return attendance;
    }

    private StudentCourseRecord createStudentCourseRecord(UUID recordId, UUID studentId, UUID courseId) {
        StudentCourseRecord record = StudentCourseRecord.create(studentId, courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", recordId);
        return record;
    }

    private Course createCourse(UUID courseId, UUID teacherId) {
        Course course = Course.create(
                UUID.randomUUID(),
                teacherId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}
