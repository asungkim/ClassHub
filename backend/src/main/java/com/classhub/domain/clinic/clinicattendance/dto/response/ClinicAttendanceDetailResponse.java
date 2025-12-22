package com.classhub.domain.clinic.clinicattendance.dto.response;

import java.util.UUID;

public record ClinicAttendanceDetailResponse(
        UUID attendanceId,
        UUID recordId,
        UUID studentCourseRecordId,
        UUID studentMemberId,
        String studentName,
        String phoneNumber,
        String schoolName,
        String grade,
        String parentPhoneNumber,
        Integer age
) {
}
