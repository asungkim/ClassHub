package com.classhub.domain.member.dto.response;

import com.classhub.domain.member.model.Member;
import java.util.List;
import java.util.UUID;

public record TeacherSearchResponse(
        UUID teacherId,
        String teacherName,
        List<TeacherBranchSummary> branches
) {

    public static TeacherSearchResponse from(Member teacher, List<TeacherBranchSummary> branches) {
        return new TeacherSearchResponse(
                teacher.getId(),
                teacher.getName(),
                branches
        );
    }

    public record TeacherBranchSummary(
            UUID companyId,
            String companyName,
            UUID branchId,
            String branchName
    ) {
    }
}
