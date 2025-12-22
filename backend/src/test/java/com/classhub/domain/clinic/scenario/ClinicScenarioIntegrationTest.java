package com.classhub.domain.clinic.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.clinic.attendance.application.ClinicAttendanceService;
import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.batch.application.ClinicBatchService;
import com.classhub.domain.clinic.session.application.ClinicSessionService;
import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.model.ClinicSessionType;
import com.classhub.domain.clinic.session.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClinicScenarioIntegrationTest {

    @Autowired
    private ClinicBatchService clinicBatchService;

    @Autowired
    private ClinicAttendanceService clinicAttendanceService;

    @Autowired
    private ClinicSessionService clinicSessionService;

    @Autowired
    private ClinicSlotRepository clinicSlotRepository;

    @Autowired
    private ClinicSessionRepository clinicSessionRepository;

    @Autowired
    private ClinicAttendanceRepository clinicAttendanceRepository;

    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void weeklyBatch_shouldCreateSessionsAndAttendances() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(branchId, teacherId));
        ClinicSlot slot = clinicSlotRepository.save(ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(3)
                .build());
        studentCourseRecordRepository.save(StudentCourseRecord.create(
                studentId,
                course.getId(),
                null,
                slot.getId(),
                null
        ));

        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        clinicBatchService.generateWeeklySessions(baseDate);
        clinicBatchService.generateWeeklyAttendances(baseDate);

        ClinicSession session = clinicSessionRepository
                .findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), LocalDate.of(2024, Month.MARCH, 4))
                .orElseThrow();
        List<ClinicAttendance> attendances = clinicAttendanceRepository.findByClinicSessionId(session.getId());

        assertThat(attendances).hasSize(1);
        assertThat(attendances.get(0).getStudentCourseRecordId()).isNotNull();
    }

    @Test
    void studentMoveAttendance_shouldMoveWithinSameWeek() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(branchId, teacherId));
        StudentCourseRecord record = studentCourseRecordRepository.save(StudentCourseRecord.create(
                studentId,
                course.getId(),
                null,
                null,
                null
        ));

        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ClinicSession fromSession = clinicSessionRepository.save(createSession(
                teacherId,
                branchId,
                startOfWeek,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        ));
        ClinicSession toSession = clinicSessionRepository.save(createSession(
                teacherId,
                branchId,
                startOfWeek.plusDays(2),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        ));
        clinicAttendanceRepository.save(ClinicAttendance.builder()
                .clinicSessionId(fromSession.getId())
                .studentCourseRecordId(record.getId())
                .build());

        MemberPrincipal principal = new MemberPrincipal(studentId, MemberRole.STUDENT);
        ClinicAttendance moved = clinicAttendanceService
                .moveAttendance(principal, fromSession.getId(), toSession.getId());

        assertThat(moved.getClinicSessionId()).isEqualTo(toSession.getId());
        assertThat(clinicAttendanceRepository.findByClinicSessionId(fromSession.getId())).isEmpty();
        assertThat(clinicAttendanceRepository.findByClinicSessionId(toSession.getId())).hasSize(1);
    }

    @Test
    void cancelSession_shouldKeepAttendances() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentCourseRecord record = studentCourseRecordRepository.save(StudentCourseRecord.create(
                studentId,
                courseRepository.save(createCourse(branchId, teacherId)).getId(),
                null,
                null,
                null
        ));
        ClinicSession session = clinicSessionRepository.save(createSession(
                teacherId,
                branchId,
                LocalDate.now().plusDays(1),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        ));
        ClinicAttendance attendance = clinicAttendanceRepository.save(ClinicAttendance.builder()
                .clinicSessionId(session.getId())
                .studentCourseRecordId(record.getId())
                .build());

        clinicSessionService.cancelSession(new MemberPrincipal(teacherId, MemberRole.TEACHER), session.getId());

        ClinicSession updated = clinicSessionRepository.findByIdAndDeletedAtIsNull(session.getId()).orElseThrow();
        assertThat(updated.isCanceled()).isTrue();
        assertThat(clinicAttendanceRepository.findById(attendance.getId())).isPresent();
    }

    private Course createCourse(UUID branchId, UUID teacherId) {
        return Course.create(
                branchId,
                teacherId,
                "Course",
                "desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
    }

    private ClinicSession createSession(UUID teacherId,
                                        UUID branchId,
                                        LocalDate date,
                                        LocalTime startTime,
                                        LocalTime endTime) {
        return ClinicSession.builder()
                .slotId(UUID.randomUUID())
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .capacity(5)
                .canceled(false)
                .build();
    }
}
