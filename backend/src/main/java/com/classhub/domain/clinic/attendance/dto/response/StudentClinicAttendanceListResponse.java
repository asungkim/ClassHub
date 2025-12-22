package com.classhub.domain.clinic.attendance.dto.response;

import java.util.List;

public record StudentClinicAttendanceListResponse(
        List<StudentClinicAttendanceResponse> items
) {
}
