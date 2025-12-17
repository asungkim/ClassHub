package com.classhub.domain.clinic.clinicslot.dto;

import jakarta.validation.constraints.*;
import java.time.DayOfWeek;

public record ClinicSlotUpdateRequest(
                DayOfWeek dayOfWeek,

                @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm 입니다") String startTime,

                @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm 입니다") String endTime,

                @Min(value = 1, message = "정원은 최소 1명입니다") @Max(value = 100, message = "정원은 최대 50명입니다") Integer capacity) {
}
