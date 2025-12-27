package com.classhub.global.init.seeds;

import com.classhub.domain.member.model.StudentGrade;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Default StudentInfo seed definitions used for initial data bootstrapping.
 */
public final class InitStudentInfos {

    private InitStudentInfos() {
    }

    public static List<StudentInfoSeed> seeds() {
        List<StudentInfoSeed> seeds = new ArrayList<>();
        StudentGrade[] grades = {
                StudentGrade.ELEMENTARY_4,
                StudentGrade.ELEMENTARY_5,
                StudentGrade.ELEMENTARY_6,
                StudentGrade.MIDDLE_1,
                StudentGrade.MIDDLE_2,
                StudentGrade.MIDDLE_3,
                StudentGrade.HIGH_1,
                StudentGrade.HIGH_2,
                StudentGrade.HIGH_3,
                StudentGrade.GAP_YEAR
        };
        String[] schoolNames = {
                "서울초등학교",
                "부산초등학교",
                "대구중학교",
                "광주중학교",
                "대전고등학교",
                "인천고등학교",
                "분당중학교",
                "대치고등학교",
                "강남고등학교",
                "수원고등학교"
        };

        for (int i = 1; i <= 100; i += 1) {
            String email = "st" + i + "@n.com";
            StudentGrade grade = grades[(i - 1) % grades.length];
            String schoolName = schoolNames[(i - 1) % schoolNames.length];
            LocalDate birthDate = LocalDate.of(2004 + (i % 10), ((i - 1) % 12) + 1, ((i - 1) % 28) + 1);
            String parentPhone = String.format("010-9%03d-%04d", 100 + (i % 900), 1000 + (i % 9000));
            seeds.add(new StudentInfoSeed(email, schoolName, grade, birthDate, parentPhone));
        }

        return List.copyOf(seeds);
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
