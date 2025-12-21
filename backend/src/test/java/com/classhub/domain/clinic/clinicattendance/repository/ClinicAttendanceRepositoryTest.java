package com.classhub.domain.clinic.clinicattendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicrecord.model.ClinicRecord;
import com.classhub.domain.clinic.clinicrecord.repository.ClinicRecordRepository;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ClinicAttendanceRepositoryTest {

    @Autowired
    private ClinicAttendanceRepository attendanceRepository;
    @Autowired
    private ClinicSessionRepository sessionRepository;
    @Autowired
    private ClinicSlotRepository slotRepository;
    @Autowired
    private ClinicRecordRepository recordRepository;
    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Autowired
    private CourseRepository courseRepository;

    private UUID teacherId;
    private UUID studentId;
    private StudentCourseRecord record;
    private ClinicSlot slot;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        Course course = courseRepository.save(createCourse(teacherId));
        record = studentCourseRecordRepository.save(
                StudentCourseRecord.create(studentId, course.getId(), null, null, null)
        );
        slot = slotRepository.save(createSlot(course.getId(), teacherId));
    }

    @Test
    @DisplayName("학생 기록 기준 클리닉 이벤트 조회는 기간 필터와 날짜 순서를 따른다")
    void findEventsByRecordIdsAndDateRange_shouldReturnOrderedEvents() {
        LocalDate start = LocalDate.of(2024, Month.MARCH, 1);
        LocalDate end = LocalDate.of(2024, Month.MARCH, 31);

        ClinicSession earlySession = sessionRepository.save(createSession(slot.getId(), LocalDate.of(2024, 3, 2)));
        ClinicAttendance earlyAttendance = attendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(earlySession.getId())
                        .studentCourseRecordId(record.getId())
                        .build()
        );

        ClinicSession lateSession = sessionRepository.save(createSession(slot.getId(), LocalDate.of(2024, 3, 5)));
        ClinicAttendance lateAttendance = attendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(lateSession.getId())
                        .studentCourseRecordId(record.getId())
                        .build()
        );
        recordRepository.save(
                ClinicRecord.builder()
                        .clinicAttendanceId(lateAttendance.getId())
                        .writerId(teacherId)
                        .title("Record")
                        .content("memo")
                        .build()
        );

        StudentCourseRecord otherRecord = studentCourseRecordRepository.save(
                StudentCourseRecord.create(UUID.randomUUID(), record.getCourseId(), null, null, null)
        );
        ClinicSession otherSession = sessionRepository.save(createSession(slot.getId(), LocalDate.of(2024, 3, 4)));
        attendanceRepository.save(
                ClinicAttendance.builder()
                        .clinicSessionId(otherSession.getId())
                        .studentCourseRecordId(otherRecord.getId())
                        .build()
        );

        List<ClinicAttendanceEventProjection> results = attendanceRepository
                .findEventsByRecordIdsAndDateRange(List.of(record.getId()), start, end);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDate()).isEqualTo(LocalDate.of(2024, 3, 2));
        assertThat(results.get(0).getRecordId()).isNull();
        assertThat(results.get(1).getDate()).isEqualTo(LocalDate.of(2024, 3, 5));
        assertThat(results.get(1).getRecordTitle()).isEqualTo("Record");
        assertThat(results.get(1).getRecordContent()).isEqualTo("memo");
        assertThat(results.get(1).getStartTime()).isEqualTo(slot.getStartTime());
        assertThat(results.get(1).getEndTime()).isEqualTo(slot.getEndTime());
    }

    private Course createCourse(UUID ownerId) {
        return Course.create(
                UUID.randomUUID(),
                ownerId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
    }

    private ClinicSlot createSlot(UUID courseId, UUID teacherId) {
        return ClinicSlot.builder()
                .courseId(courseId)
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(UUID.randomUUID())
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(10)
                .build();
    }

    private ClinicSession createSession(UUID slotId, LocalDate date) {
        return ClinicSession.builder()
                .slotId(slotId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .capacity(10)
                .canceled(false)
                .build();
    }
}
