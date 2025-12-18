package com.classhub.global.init;

public final class SeedKeys {

    private SeedKeys() {
    }

    public static final String SUPER_ADMIN = "superadmin";
    public static final String TEACHER_ALPHA = "teacher-alpha";
    public static final String TEACHER_BETA = "teacher-beta";

    public static String assistantKey(String teacherKey, int index) {
        return teacherKey + "-assistant-" + index;
    }

    public static String studentMemberKey(String teacherKey) {
        return teacherKey + "-student-account";
    }

    public static String courseKey(String teacherKey, int index) {
        return teacherKey + "-course-" + index;
    }

    public static String studentProfileKey(String teacherKey, int index) {
        return teacherKey + "-student-profile-" + index;
    }
}
