package com.classhub.domain.clinic.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.model.ClinicSessionType;
import com.classhub.domain.clinic.session.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.config.JpaConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ClinicAttendanceRepositoryQueryTest {

    @Autowired
    private ClinicAttendanceRepository clinicAttendanceRepository;
    @Autowired
    private ClinicSessionRepository clinicSessionRepository;
    @Autowired
    private ClinicSlotRepository clinicSlotRepository;
    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Autowired
    private CourseRepository courseRepository;

    @Test
    void findByClinicSessionId_shouldReturnAttendances() {
        UUID teacherId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(teacherId));
        StudentCourseRecord record = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, null, null)
        );
        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId));
        ClinicSession session = clinicSessionRepository.save(createSession(slot, teacherId, slot.getBranchId()));
        ClinicAttendance attendance = clinicAttendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(session.getId())
                        .studentCourseRecordId(record.getId())
                        .build()
        );

        var results = clinicAttendanceRepository.findByClinicSessionId(session.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(attendance.getId());
    }

    @Test
    void countOverlappingAttendances_shouldCountOverlaps() {
        UUID teacherId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(teacherId));
        StudentCourseRecord record = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, null, null)
        );
        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId));
        ClinicSession session = clinicSessionRepository.save(createSession(slot, teacherId, slot.getBranchId()));
        clinicAttendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(session.getId())
                        .studentCourseRecordId(record.getId())
                        .build()
        );

        long count = clinicAttendanceRepository.countOverlappingAttendances(
                record.getId(),
                session.getDate(),
                LocalTime.of(18, 30),
                LocalTime.of(19, 30)
        );

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void findAttendanceCountsByClinicSessionIds_shouldReturnCounts() {
        UUID teacherId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(teacherId));
        StudentCourseRecord recordOne = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, null, null)
        );
        StudentCourseRecord recordTwo = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, null, null)
        );
        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId));
        ClinicSession sessionOne = clinicSessionRepository.save(createSession(slot, teacherId, slot.getBranchId()));
        ClinicSession sessionTwo = clinicSessionRepository.save(createSession(slot, teacherId, slot.getBranchId()));
        clinicAttendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(sessionOne.getId())
                        .studentCourseRecordId(recordOne.getId())
                        .build()
        );
        clinicAttendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(sessionOne.getId())
                        .studentCourseRecordId(recordTwo.getId())
                        .build()
        );
        clinicAttendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(sessionTwo.getId())
                        .studentCourseRecordId(recordOne.getId())
                        .build()
        );

        var results = clinicAttendanceRepository.findAttendanceCountsByClinicSessionIds(
                List.of(sessionOne.getId(), sessionTwo.getId())
        );

        assertThat(results)
                .hasSize(2)
                .anySatisfy(result -> {
                    if (result.getClinicSessionId().equals(sessionOne.getId())) {
                        assertThat(result.getAttendanceCount()).isEqualTo(2L);
                    }
                })
                .anySatisfy(result -> {
                    if (result.getClinicSessionId().equals(sessionTwo.getId())) {
                        assertThat(result.getAttendanceCount()).isEqualTo(1L);
                    }
                });
    }

    @Test
    void deleteUpcomingAttendances_shouldRemoveOnlyFutureSessions() {
        UUID teacherId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(teacherId));
        StudentCourseRecord record = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), course.getId(), null, null, null)
        );
        ClinicSlot slot = clinicSlotRepository.save(createSlot(teacherId));
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 5);
        LocalTime thresholdTime = LocalTime.of(12, 0);
        ClinicSession beforeSession = clinicSessionRepository.save(
                createSession(slot, teacherId, slot.getBranchId(), baseDate, LocalTime.of(9, 0))
        );
        ClinicSession atSession = clinicSessionRepository.save(
                createSession(slot, teacherId, slot.getBranchId(), baseDate, thresholdTime)
        );
        ClinicSession afterSession = clinicSessionRepository.save(
                createSession(slot, teacherId, slot.getBranchId(), baseDate.plusDays(1), LocalTime.of(10, 0))
        );
        clinicAttendanceRepository.save(attendanceFor(record.getId(), beforeSession.getId()));
        clinicAttendanceRepository.save(attendanceFor(record.getId(), atSession.getId()));
        clinicAttendanceRepository.save(attendanceFor(record.getId(), afterSession.getId()));

        int deleted = clinicAttendanceRepository.deleteUpcomingAttendances(record.getId(), baseDate, thresholdTime);

        assertThat(deleted).isEqualTo(2);
        List<ClinicAttendance> remaining = clinicAttendanceRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getClinicSessionId()).isEqualTo(beforeSession.getId());
    }

    private Course createCourse(UUID teacherId) {
        return Course.create(
                UUID.randomUUID(),
                teacherId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
    }

    private ClinicSlot createSlot(UUID teacherId) {
        return ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(UUID.randomUUID())
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(10)
                .build();
    }

    private ClinicSession createSession(ClinicSlot slot, UUID teacherId, UUID branchId) {
        return ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(LocalDate.of(2024, Month.MARCH, 5))
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();
    }

    private ClinicSession createSession(ClinicSlot slot,
                                        UUID teacherId,
                                        UUID branchId,
                                        LocalDate date,
                                        LocalTime startTime) {
        return ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();
    }

    private ClinicAttendance attendanceFor(UUID recordId, UUID sessionId) {
        return ClinicAttendance.builder()
                .clinicSessionId(sessionId)
                .studentCourseRecordId(recordId)
                .build();
    }
}
