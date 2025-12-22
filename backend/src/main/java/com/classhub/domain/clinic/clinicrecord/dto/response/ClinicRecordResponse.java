package com.classhub.domain.clinic.clinicrecord.dto.response;

import com.classhub.domain.clinic.clinicrecord.model.ClinicRecord;
import java.util.UUID;

public record ClinicRecordResponse(
        UUID recordId,
        UUID clinicAttendanceId,
        UUID writerId,
        String title,
        String content,
        String homeworkProgress
) {

    public static ClinicRecordResponse from(ClinicRecord record) {
        return new ClinicRecordResponse(
                record.getId(),
                record.getClinicAttendanceId(),
                record.getWriterId(),
                record.getTitle(),
                record.getContent(),
                record.getHomeworkProgress()
        );
    }
}
