package com.classhub.global.init.data;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class StudentProfileInitData extends BaseInitData {

    private static final String[] FAMILY_NAMES = {"김", "이", "박", "최", "정", "한", "장", "조", "윤", "임"};
    private static final String[] GIVEN_FIRST = {"서", "민", "도", "하", "예", "지", "현", "승", "시", "채"};
    private static final String[] GIVEN_SECOND = {"연", "준", "윤", "율", "빈", "진", "람", "후", "아", "솔"};
    private static final List<String> SEOUL_SCHOOLS = List.of(
            "대치고등학교",
            "휘문고등학교",
            "청담중학교",
            "풍문여자고등학교",
            "잠신중학교",
            "중동고등학교"
    );
    private static final List<String> BUNDANG_SCHOOLS = List.of(
            "분당정자중학교",
            "분당서울고등학교",
            "정자고등학교",
            "서현중학교",
            "수내고등학교",
            "야탑중학교"
    );
    private static final GradeBand[] GRADE_BANDS = {
            new GradeBand("중2", 14),
            new GradeBand("중3", 15),
            new GradeBand("고1", 16),
            new GradeBand("고2", 17),
            new GradeBand("고3", 18),
            new GradeBand("재수", 19)
    };

    private final StudentProfileRepository studentProfileRepository;
    private final StudentCourseEnrollmentRepository studentCourseEnrollmentRepository;
    private final BootstrapSeedContext seedContext;

    public StudentProfileInitData(
            StudentProfileRepository studentProfileRepository,
            StudentCourseEnrollmentRepository studentCourseEnrollmentRepository,
            BootstrapSeedContext seedContext
    ) {
        super("student-profile-seed", 300);
        this.studentProfileRepository = studentProfileRepository;
        this.studentCourseEnrollmentRepository = studentCourseEnrollmentRepository;
        this.seedContext = seedContext;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        seedForTeacher(SeedKeys.TEACHER_ALPHA, "Alpha", SeedKeys.studentMemberKey(SeedKeys.TEACHER_ALPHA), 200);
        seedForTeacher(SeedKeys.TEACHER_BETA, "Beta", SeedKeys.studentMemberKey(SeedKeys.TEACHER_BETA), 400);
    }

    private void seedForTeacher(String teacherKey, String label, String studentMemberKey, int phoneBlock) {
        Member teacher = seedContext.getRequiredMember(teacherKey);
        List<String> assistantKeys = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            assistantKeys.add(SeedKeys.assistantKey(teacherKey, i));
        }
        List<String> courseKeys = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            courseKeys.add(SeedKeys.courseKey(teacherKey, i));
        }

        for (int i = 1; i <= 30; i++) {
            String profileKey = SeedKeys.studentProfileKey(teacherKey, i);
            String assistantKey = assistantKeys.get((i - 1) % assistantKeys.size());
            String courseKey = courseKeys.get((i - 1) % courseKeys.size());
            StudentProfileSeed seed = buildSeed(
                    profileKey,
                    teacher,
                    assistantKey,
                    courseKey,
                    label,
                    i,
                    i == 1 ? studentMemberKey : null,
                    phoneBlock
            );
            persistStudentProfile(seed);
        }
    }

    private StudentProfileSeed buildSeed(
            String key,
            Member teacher,
            String assistantKey,
            String courseKey,
            String label,
            int index,
            String memberKey,
            int phoneBlock
    ) {
        String studentName = buildKoreanName(index, label);
        String phoneNumber = String.format("010-%03d-%04d", phoneBlock + index, 1000 + index);
        String parentPhone = String.format("010-%03d-%04d", phoneBlock + 300 + index, 2000 + index);
        String schoolName = resolveSchoolName(label, index);
        GradeBand gradeBand = GRADE_BANDS[(index - 1) % GRADE_BANDS.length];
        String grade = gradeBand.grade();
        int age = gradeBand.age();
        return new StudentProfileSeed(
                key,
                teacher,
                assistantKey,
                courseKey,
                studentName,
                phoneNumber,
                parentPhone,
                schoolName,
                grade,
                age,
                memberKey
        );
    }

    private String buildKoreanName(int index, String label) {
        int base = index - 1;
        String surname = FAMILY_NAMES[base % FAMILY_NAMES.length];
        String givenFirst = GIVEN_FIRST[(base + ("Alpha".equals(label) ? 0 : 3)) % GIVEN_FIRST.length];
        String givenSecond = GIVEN_SECOND[(base * 2) % GIVEN_SECOND.length];
        return surname + givenFirst + givenSecond;
    }

    private String resolveSchoolName(String label, int index) {
        List<String> schools = "Alpha".equals(label) ? SEOUL_SCHOOLS : BUNDANG_SCHOOLS;
        return schools.get((index - 1) % schools.size());
    }

    private void persistStudentProfile(StudentProfileSeed seed) {
        Course course = seedContext.getRequiredCourse(seed.courseKey());
        Member assistant = seedContext.getRequiredMember(seed.assistantKey());
        UUID memberId = Optional.ofNullable(seed.memberKey())
                .flatMap(seedContext::findMember)
                .map(Member::getId)
                .orElse(null);

        StudentProfile profile = studentProfileRepository
                .findByTeacherIdAndPhoneNumberIgnoreCase(seed.teacher().getId(), seed.phoneNumber())
                .map(existing -> updateProfile(existing, seed, assistant.getId(), memberId))
                .orElseGet(() -> createProfile(seed, assistant.getId(), memberId));
        syncEnrollment(profile, course.getId(), seed.teacher().getId());
        seedContext.storeStudentProfile(seed.key(), profile);
    }

    private StudentProfile updateProfile(
            StudentProfile profile,
            StudentProfileSeed seed,
            UUID assistantId,
            UUID memberId
    ) {
        profile.updateBasicInfo(seed.name(), seed.parentPhone(), seed.schoolName(), seed.grade(), seed.age());
        profile.assignAssistant(assistantId);
        if (memberId != null) {
            profile.assignMember(memberId);
        }
        profile.activate();
        return profile;
    }

    private StudentProfile createProfile(
            StudentProfileSeed seed,
            UUID assistantId,
            UUID memberId
    ) {
        StudentProfile.StudentProfileBuilder builder = StudentProfile.builder()
                .teacherId(seed.teacher().getId())
                .assistantId(assistantId)
                .name(seed.name())
                .phoneNumber(seed.phoneNumber())
                .parentPhone(seed.parentPhone())
                .schoolName(seed.schoolName())
                .grade(seed.grade())
                .age(seed.age());

        if (memberId != null) {
            builder.memberId(memberId);
        }

        return studentProfileRepository.save(builder.build());
    }

    private void syncEnrollment(StudentProfile profile, UUID courseId, UUID teacherId) {
        List<StudentCourseEnrollment> enrollments =
                studentCourseEnrollmentRepository.findAllByStudentProfileId(profile.getId());
        enrollments.stream()
                .filter(enrollment -> !enrollment.getCourseId().equals(courseId))
                .forEach(enrollment -> studentCourseEnrollmentRepository
                        .deleteByStudentProfileIdAndCourseId(profile.getId(), enrollment.getCourseId()));
        boolean alreadyAssigned = enrollments.stream()
                .anyMatch(enrollment -> enrollment.getCourseId().equals(courseId));
        if (!alreadyAssigned) {
            studentCourseEnrollmentRepository.save(
                    StudentCourseEnrollment.builder()
                            .studentProfileId(profile.getId())
                            .courseId(courseId)
                            .teacherId(teacherId)
                            .build()
            );
        }
    }

    private record StudentProfileSeed(
            String key,
            Member teacher,
            String assistantKey,
            String courseKey,
            String name,
            String phoneNumber,
            String parentPhone,
            String schoolName,
            String grade,
            int age,
            String memberKey
    ) {
    }

    private record GradeBand(String grade, int age) {
    }
}
