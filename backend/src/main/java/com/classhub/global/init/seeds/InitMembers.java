package com.classhub.global.init.seeds;

import com.classhub.domain.member.model.MemberRole;
import java.util.ArrayList;
import java.util.List;

/**
 * Default Member seed definitions used for initial data bootstrapping.
 */
public final class InitMembers {

    public static final String DEFAULT_PASSWORD = "Qwer123!";

    private InitMembers() {
    }

    public static List<MemberSeed> seeds() {
        List<MemberSeed> seeds = new ArrayList<>();
        seeds.add(buildSeed("admin", "ad@n.com", "김관리", buildPhone(0, 1), MemberRole.SUPER_ADMIN));
        seeds.add(buildSeed("teacher-1", "te1@n.com", "김철수", buildPhone(1000, 1), MemberRole.TEACHER));
        seeds.add(buildSeed("teacher-2", "te2@n.com", "이영희", buildPhone(1000, 2), MemberRole.TEACHER));

        seeds.add(buildSeed("assistant-1", "as1@n.com", "박민수", buildPhone(2000, 1), MemberRole.ASSISTANT));
        seeds.add(buildSeed("assistant-2", "as2@n.com", "최지은", buildPhone(2000, 2), MemberRole.ASSISTANT));
        seeds.add(buildSeed("assistant-3", "as3@n.com", "정다은", buildPhone(2000, 3), MemberRole.ASSISTANT));
        seeds.add(buildSeed("assistant-4", "as4@n.com", "한지민", buildPhone(2000, 4), MemberRole.ASSISTANT));

        for (int i = 1; i <= 100; i += 1) {
            String email = "st" + i + "@n.com";
            String name = buildStudentName(i);
            String phone = buildPhone(3000, i);
            seeds.add(buildSeed("student-" + i, email, name, phone, MemberRole.STUDENT));
        }

        return List.copyOf(seeds);
    }

    private static MemberSeed buildSeed(String key,
                                        String email,
                                        String name,
                                        String phoneNumber,
                                        MemberRole role) {
        return new MemberSeed(key, email, name, phoneNumber, role, DEFAULT_PASSWORD);
    }

    private static String buildPhone(int prefix, int suffix) {
        return String.format("010-%04d-%04d", prefix, suffix);
    }

    private static String buildStudentName(int index) {
        String[] familyNames = {
                "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
                "한", "오", "서", "신", "권", "황", "안", "송", "전", "홍"
        };
        String[] givenNames = {
                "민준", "서준", "도윤", "예준", "시우", "하준", "지호", "지후", "준서", "준우",
                "현우", "지훈", "선우", "서연", "서윤", "지우", "지민", "서현", "민서", "하은",
                "윤서", "채원", "지아", "지안", "지윤", "하린", "예은", "수아", "다은", "소율"
        };
        String family = familyNames[(index - 1) % familyNames.length];
        String given = givenNames[(index - 1) % givenNames.length];
        return family + given;
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
