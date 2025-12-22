package com.classhub.domain.studentcourse.dto.response;

import java.util.UUID;

public record StudentDefaultClinicSlotResponse(
        UUID studentCourseRecordId,
        UUID defaultClinicSlotId
) {
}
