package com.classhub.domain.studentcourse.web;

import com.classhub.domain.clinic.slot.application.ClinicDefaultSlotService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.studentcourse.dto.request.StudentDefaultClinicSlotRequest;
import com.classhub.domain.studentcourse.dto.response.StudentDefaultClinicSlotResponse;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/students/me/courses")
@Tag(name = "Student Course API", description = "학생 기본 클리닉 슬롯 관리 API")
public class StudentCourseController {

    private final ClinicDefaultSlotService clinicDefaultSlotService;

    @PatchMapping("/{courseId}/clinic-slot")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "학생 기본 클리닉 슬롯 변경")
    public RsData<StudentDefaultClinicSlotResponse> updateDefaultClinicSlot(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody StudentDefaultClinicSlotRequest request
    ) {
        StudentCourseRecord record = clinicDefaultSlotService
                .updateDefaultSlotForStudent(principal.id(), courseId, request.defaultClinicSlotId());
        StudentDefaultClinicSlotResponse response = new StudentDefaultClinicSlotResponse(
                record.getId(),
                record.getDefaultClinicSlotId()
        );
        return RsData.from(RsCode.SUCCESS, response);
    }
}
