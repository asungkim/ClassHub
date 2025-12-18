package com.classhub.global.init.seeds;

import com.classhub.domain.member.model.MemberRole;
import java.util.List;

/**
 * Default Member seed definitions used for initial data bootstrapping.
 */
public final class InitMembers {

    private InitMembers() {
    }

    public static List<MemberSeed> seeds() {
        return List.of(
                new MemberSeed(
                        "super-admin",
                        "superadmin@classhub.dev",
                        "ClassHub SuperAdmin",
                        "010-0000-0001",
                        MemberRole.SUPER_ADMIN,
                        "Admin!123"
                ),
                new MemberSeed(
                        "teacher-alice",
                        "teacher.alice@classhub.dev",
                        "Alice Park",
                        "010-1000-2000",
                        MemberRole.TEACHER,
                        "Teacher!123"
                ),
                new MemberSeed(
                        "teacher-bob",
                        "teacher.bob@classhub.dev",
                        "Bob Kim",
                        "010-1000-3000",
                        MemberRole.TEACHER,
                        "Teacher!123"
                ),
                new MemberSeed(
                        "assistant-mina",
                        "assistant.mina@classhub.dev",
                        "Mina Choi",
                        "010-2000-1000",
                        MemberRole.ASSISTANT,
                        "Assistant!123"
                ),
                new MemberSeed(
                        "assistant-jisoo",
                        "assistant.jisoo@classhub.dev",
                        "Jisoo Han",
                        "010-2000-2000",
                        MemberRole.ASSISTANT,
                        "Assistant!123"
                ),
                new MemberSeed(
                        "student-jaekyung",
                        "student.jaekyung@classhub.dev",
                        "JaeKyung Lee",
                        "010-3000-1000",
                        MemberRole.STUDENT,
                        "Student!123"
                ),
                new MemberSeed(
                        "student-arin",
                        "student.arin@classhub.dev",
                        "Arin Seo",
                        "010-3000-2000",
                        MemberRole.STUDENT,
                        "Student!123"
                ),
                new MemberSeed(
                        "student-donghyuk",
                        "student.donghyuk@classhub.dev",
                        "Donghyuk Jung",
                        "010-3000-3000",
                        MemberRole.STUDENT,
                        "Student!123"
                ),
                new MemberSeed(
                        "student-sumin",
                        "student.sumin@classhub.dev",
                        "Sumin Cho",
                        "010-3000-4000",
                        MemberRole.STUDENT,
                        "Student!123"
                )
        );
    }

    public record MemberSeed(
            String key,
            String email,
            String name,
            String phoneNumber,
            MemberRole role,
            String rawPassword
    ) {
    }
}
