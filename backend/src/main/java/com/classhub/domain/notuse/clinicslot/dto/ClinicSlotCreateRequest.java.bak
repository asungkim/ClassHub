package com.classhub.domain.clinic.clinicslot.dto;

import jakarta.validation.constraints.*;
import java.time.DayOfWeek;

public record ClinicSlotCreateRequest(
                @NotNull(message = "요일은 필수입니다") DayOfWeek dayOfWeek,
                @NotBlank(message = "시작 시간은 필수입니다") @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm 입니다") String startTime,

                @NotBlank(message = "종료 시간은 필수입니다") @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm 입니다") String endTime,

                @Min(value = 1, message = "정원은 최소 1명입니다") @Max(value = 100, message = "정원은 최대 50명입니다") int capacity) {
}
