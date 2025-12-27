package com.classhub.global.init.seeds;

import com.classhub.domain.assignment.model.BranchRole;
import java.util.List;

public final class InitTeacherBranchAssignments {

    private InitTeacherBranchAssignments() {
    }

    public static List<TeacherBranchSeed> seeds() {
        return List.of(
                new TeacherBranchSeed(
                        "te1@n.com",
                        "러셀",
                        List.of("강남", "대치"),
                        BranchRole.FREELANCE
                ),
                new TeacherBranchSeed(
                        "te2@n.com",
                        "두각",
                        List.of("본관", "태성관"),
                        BranchRole.FREELANCE
                )
        );
    }

    public record TeacherBranchSeed(
            String teacherEmail,
            String companyName,
            List<String> branchNames,
            BranchRole role
    ) {
    }
}
