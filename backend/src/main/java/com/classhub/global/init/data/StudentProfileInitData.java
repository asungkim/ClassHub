package com.classhub.global.init.data;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class StudentProfileInitData extends BaseInitData {

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
        String studentName = label + " Student " + index;
        String phoneNumber = String.format("010-%03d-%04d", phoneBlock + index, 1000 + index);
        String parentPhone = String.format("010-%03d-%04d", phoneBlock + 300 + index, 2000 + index);
        String schoolName = label + " Middle School " + ((index % 5) + 1);
        String grade = "G" + ((index % 3) + 1);
        int age = 13 + (index % 5);
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
}
