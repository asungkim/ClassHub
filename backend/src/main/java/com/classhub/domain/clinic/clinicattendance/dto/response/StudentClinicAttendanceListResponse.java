package com.classhub.domain.clinic.clinicattendance.dto.response;

import java.util.List;

public record StudentClinicAttendanceListResponse(
        List<StudentClinicAttendanceResponse> items
) {
}
