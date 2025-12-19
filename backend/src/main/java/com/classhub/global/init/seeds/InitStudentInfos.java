package com.classhub.global.init.seeds;

import com.classhub.domain.member.model.StudentGrade;
import java.time.LocalDate;
import java.util.List;

/**
 * Default StudentInfo seed definitions used for initial data bootstrapping.
 */
public final class InitStudentInfos {

    private InitStudentInfos() {
    }

    public static List<StudentInfoSeed> seeds() {
        return List.of(
                new StudentInfoSeed(
                        "student.jaekyung@classhub.dev",
                        "서울과학고등학교",
                        StudentGrade.HIGH_2,
                        LocalDate.of(2008, 3, 14),
                        "010-9988-7766"
                ),
                new StudentInfoSeed(
                        "student.arin@classhub.dev",
                        "분당중학교",
                        StudentGrade.MIDDLE_2,
                        LocalDate.of(2011, 9, 2),
                        "010-4444-8888"
                ),
                new StudentInfoSeed(
                        "student.donghyuk@classhub.dev",
                        "신촌초등학교",
                        StudentGrade.ELEMENTARY_5,
                        LocalDate.of(2014, 1, 28),
                        "010-1111-5555"
                ),
                new StudentInfoSeed(
                        "student.sumin@classhub.dev",
                        "대치고등학교",
                        StudentGrade.GAP_YEAR,
                        LocalDate.of(2006, 7, 19),
                        "010-2222-6666"
                )
        );
    }

    public record StudentInfoSeed(
            String memberEmail,
            String schoolName,
            StudentGrade grade,
            LocalDate birthDate,
            String parentPhone
    ) {
    }
}
