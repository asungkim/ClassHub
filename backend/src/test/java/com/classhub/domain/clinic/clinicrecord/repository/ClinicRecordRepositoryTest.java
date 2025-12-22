package com.classhub.domain.clinic.clinicrecord.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.clinic.clinicrecord.model.ClinicRecord;
import com.classhub.global.config.JpaConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ClinicRecordRepositoryTest {

    @Autowired
    private ClinicRecordRepository clinicRecordRepository;

    @Test
    void findByClinicAttendanceId_shouldReturnRecord() {
        UUID attendanceId = UUID.randomUUID();
        ClinicRecord record = clinicRecordRepository.save(createRecord(attendanceId));

        Optional<ClinicRecord> result = clinicRecordRepository.findByClinicAttendanceId(attendanceId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(record.getId());
    }

    @Test
    void existsByClinicAttendanceId_shouldReturnTrue_whenRecordExists() {
        UUID attendanceId = UUID.randomUUID();
        clinicRecordRepository.save(createRecord(attendanceId));

        boolean exists = clinicRecordRepository.existsByClinicAttendanceId(attendanceId);

        assertThat(exists).isTrue();
    }

    private ClinicRecord createRecord(UUID attendanceId) {
        return ClinicRecord.builder()
                .clinicAttendanceId(attendanceId)
                .writerId(UUID.randomUUID())
                .title("Record")
                .content("content")
                .homeworkProgress("done")
                .build();
    }
}
