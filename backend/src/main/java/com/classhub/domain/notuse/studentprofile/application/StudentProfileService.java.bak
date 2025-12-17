package com.classhub.domain.studentprofile.application;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileSearchCondition;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.dto.response.EnrolledCourseInfo;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.dto.response.StudentProfileSummary;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentCourseEnrollmentRepository studentCourseEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public StudentProfileResponse createProfile(UUID principalId, StudentProfileCreateRequest request) {
        Member actor = getMember(principalId);
        ensureTeacher(actor);

        List<UUID> courseIds = normalizeCourseIds(request.courseIds());
        ensureCoursesOwnedByTeacher(courseIds, actor.getId());
        validateDuplicatePhoneNumber(actor.getId(), request.normalizedPhoneNumber());

        UUID assistantId = null;
        if (request.assistantId() != null) {
            assistantId = getAssistant(request.assistantId(), actor.getId()).getId();
        }

        StudentProfile profile = StudentProfile.builder()
                .teacherId(actor.getId())
                .assistantId(assistantId)
                .memberId(null)
                .name(request.normalizedName())
                .phoneNumber(request.normalizedPhoneNumber())
                .parentPhone(request.parentPhone())
                .schoolName(request.schoolName())
                .grade(request.grade())
                .age(request.age())
                .defaultClinicSlotId(request.defaultClinicSlotId())
                .build();

        StudentProfile saved = studentProfileRepository.save(profile);
        createEnrollments(saved.getId(), actor.getId(), courseIds);
        return StudentProfileResponse.of(saved, buildEnrolledCourses(saved.getId()));
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(UUID principalId, UUID profileId) {
        Member actor = getMember(principalId);
        UUID teacherId = resolveTeacherId(actor);
        StudentProfile profile = getProfileForTeacher(teacherId, profileId);
        return StudentProfileResponse.of(profile, buildEnrolledCourses(profile.getId()));
    }

    @Transactional(readOnly = true)
    public Page<StudentProfileSummary> getProfiles(
            UUID principalId,
            StudentProfileSearchCondition condition,
            Pageable pageable
    ) {
        Member actor = getMember(principalId);
        UUID teacherId = resolveTeacherId(actor);
        Boolean active = condition != null ? condition.active() : null;
        Page<StudentProfile> page;
        if (condition != null && condition.hasCourseFilter()) {
            ensureCourseOwnership(condition.courseId(), teacherId);
            List<UUID> profileIds = extractProfileIdsForCourse(condition.courseId());
            if (profileIds.isEmpty()) {
                page = Page.empty(pageable);
            } else if (condition.hasNameFilter()) {
                page = active == null
                        ? studentProfileRepository.findAllByTeacherIdAndIdInAndNameContainingIgnoreCase(
                                teacherId,
                                profileIds,
                                condition.name(),
                                pageable
                        )
                        : studentProfileRepository.findAllByTeacherIdAndActiveAndIdInAndNameContainingIgnoreCase(
                                teacherId,
                                active,
                                profileIds,
                                condition.name(),
                                pageable
                        );
            } else {
                page = active == null
                        ? studentProfileRepository.findAllByTeacherIdAndIdIn(teacherId, profileIds, pageable)
                        : studentProfileRepository.findAllByTeacherIdAndActiveAndIdIn(
                                teacherId,
                                active,
                                profileIds,
                                pageable
                        );
            }
        } else if (condition != null && condition.hasNameFilter()) {
            page = active == null
                    ? studentProfileRepository.findAllByTeacherIdAndNameContainingIgnoreCase(
                    teacherId,
                    condition.name(),
                    pageable
            )
                    : studentProfileRepository
                            .findAllByTeacherIdAndActiveAndNameContainingIgnoreCase(
                                    teacherId,
                                    active,
                                    condition.name(),
                                    pageable
                            );
        } else {
            page = active == null
                    ? studentProfileRepository.findAllByTeacherId(teacherId, pageable)
                    : studentProfileRepository.findAllByTeacherIdAndActive(teacherId, active, pageable);
        }

        return enrichPageWithNames(page);
    }

    @Transactional
    public StudentProfileResponse updateProfile(
            UUID principalId,
            UUID profileId,
            StudentProfileUpdateRequest request
    ) {
        Member actor = getMember(principalId);
        ensureTeacher(actor);

        StudentProfile profile = getProfileForTeacher(actor.getId(), profileId);

        if (request.assistantId() != null) {
            if (!request.assistantId().equals(profile.getAssistantId())) {
                Member assistant = getAssistant(request.assistantId(), actor.getId());
                profile.assignAssistant(assistant.getId());
            }
        } else if (profile.getAssistantId() != null) {
            profile.assignAssistant(null);
        }

        if (request.courseIds() != null) {
            List<UUID> courseIds = normalizeCourseIds(request.courseIds());
            syncEnrollments(profile.getId(), actor.getId(), courseIds);
        }

        boolean phoneChangeRequested = request.phoneNumber() != null;
        String normalizedPhone = profile.getPhoneNumber();
        if (phoneChangeRequested) {
            normalizedPhone = request.phoneNumber().trim();
            phoneChangeRequested = !normalizedPhone.equals(profile.getPhoneNumber());
        }

        if (phoneChangeRequested) {
            validateDuplicatePhoneNumber(actor.getId(), normalizedPhone);
            profile.changePhoneNumber(normalizedPhone);
        }

        if (request.memberId() != null && !request.memberId().equals(profile.getMemberId())) {
            Member student = getStudentMember(request.memberId());
            ensureMemberNotLinked(student.getId());
            profile.assignMember(student.getId());
        }

        if (request.defaultClinicSlotId() != null
                || profile.getDefaultClinicSlotId() != null) {
            profile.assignDefaultClinicSlot(request.defaultClinicSlotId());
        }

        profile.updateBasicInfo(
                request.name(),
                request.parentPhone(),
                request.schoolName(),
                request.grade(),
                request.age()
        );

        StudentProfile saved = studentProfileRepository.save(profile);
        return StudentProfileResponse.of(saved, buildEnrolledCourses(saved.getId()));
    }

    @Transactional
    public void deleteProfile(UUID principalId, UUID profileId) {
        Member actor = getMember(principalId);
        ensureTeacher(actor);

        StudentProfile profile = getProfileForTeacher(actor.getId(), profileId);
        profile.deactivate();
        studentProfileRepository.save(profile);

        if (profile.getMemberId() != null) {
            memberRepository.findById(profile.getMemberId())
                    .ifPresent(member -> {
                        member.deactivate();
                        memberRepository.save(member);
                    });
        }
    }

    @Transactional
    public void activateProfile(UUID principalId, UUID profileId) {
        Member actor = getMember(principalId);
        ensureTeacher(actor);

        StudentProfile profile = getProfileForTeacher(actor.getId(), profileId);
        profile.activate();
        studentProfileRepository.save(profile);

        if (profile.getMemberId() != null) {
            memberRepository.findById(profile.getMemberId())
                    .ifPresent(member -> {
                        member.activate();
                        memberRepository.save(member);
                    });
        }
    }

    @Transactional(readOnly = true)
    public List<StudentProfileSummary> getCourseStudents(UUID teacherId, UUID courseId) {
        ensureCourseOwnership(courseId, teacherId);
        List<UUID> profileIds = extractProfileIdsForCourse(courseId);
        if (profileIds.isEmpty()) {
            return List.of();
        }
        List<StudentProfile> profiles = studentProfileRepository.findAllById(profileIds)
                .stream()
                .filter(profile -> teacherId.equals(profile.getTeacherId()) && profile.isActive())
                .toList();
        return enrichListWithNames(profiles);
    }

    private List<UUID> extractProfileIdsForCourse(UUID courseId) {
        return studentCourseEnrollmentRepository.findAllByCourseId(courseId)
                .stream()
                .map(StudentCourseEnrollment::getStudentProfileId)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    private List<UUID> normalizeCourseIds(List<UUID> courseIds) {
        if (courseIds == null) {
            return List.of();
        }
        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(courseIds);
        if (uniqueIds.contains(null)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (uniqueIds.size() != courseIds.size()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return List.copyOf(uniqueIds);
    }

    private void ensureCoursesOwnedByTeacher(List<UUID> courseIds, UUID teacherId) {
        if (courseIds.isEmpty()) {
            return;
        }
        List<Course> courses = courseRepository.findAllById(courseIds);
        if (courses.size() != courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        boolean invalidOwner = courses.stream().anyMatch(course -> !course.isOwnedBy(teacherId));
        if (invalidOwner) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        boolean inactiveCourse = courses.stream().anyMatch(course -> !course.isActive());
        if (inactiveCourse) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
    }

    private void createEnrollments(UUID profileId, UUID teacherId, List<UUID> courseIds) {
        for (UUID courseId : courseIds) {
            studentCourseEnrollmentRepository.save(
                    StudentCourseEnrollment.builder()
                            .studentProfileId(profileId)
                            .courseId(courseId)
                            .teacherId(teacherId)
                            .build()
            );
        }
    }

    private void syncEnrollments(UUID profileId, UUID teacherId, List<UUID> courseIds) {
        if (!courseIds.isEmpty()) {
            ensureCoursesOwnedByTeacher(courseIds, teacherId);
        }
        Set<UUID> targetCourseIds = new LinkedHashSet<>(courseIds);
        List<StudentCourseEnrollment> currentEnrollments =
                studentCourseEnrollmentRepository.findAllByStudentProfileId(profileId);
        Set<UUID> currentCourseIds = currentEnrollments.stream()
                .map(StudentCourseEnrollment::getCourseId)
                .collect(Collectors.toSet());

        for (UUID courseId : targetCourseIds) {
            if (!currentCourseIds.contains(courseId)) {
                studentCourseEnrollmentRepository.save(
                        StudentCourseEnrollment.builder()
                                .studentProfileId(profileId)
                                .courseId(courseId)
                                .teacherId(teacherId)
                                .build()
                );
            }
        }

        for (UUID courseId : currentCourseIds) {
            if (!targetCourseIds.contains(courseId)) {
                studentCourseEnrollmentRepository.deleteByStudentProfileIdAndCourseId(profileId, courseId);
            }
        }
    }

    private List<EnrolledCourseInfo> buildEnrolledCourses(UUID profileId) {
        List<StudentCourseEnrollment> enrollments =
                studentCourseEnrollmentRepository.findAllByStudentProfileId(profileId);
        if (enrollments.isEmpty()) {
            return List.of();
        }
        Set<UUID> courseIds = enrollments.stream()
                .map(StudentCourseEnrollment::getCourseId)
                .collect(Collectors.toSet());
        Map<UUID, String> courseNames = courseRepository.findAllById(courseIds)
                .stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
        return enrollments.stream()
                .map(enrollment -> new EnrolledCourseInfo(
                        enrollment.getCourseId(),
                        courseNames.getOrDefault(enrollment.getCourseId(), "Unknown"),
                        enrollment.getCreatedAt()
                ))
                .toList();
    }

    private Course getCourseOwnedByTeacher(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (!course.isOwnedBy(teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        if (!course.isActive()) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        return course;
    }

    private void ensureCourseOwnership(UUID courseId, UUID teacherId) {
        if (!courseRepository.existsByIdAndTeacherId(courseId, teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
    }

    private void validateDuplicatePhoneNumber(UUID teacherId, String phoneNumber) {
        if (studentProfileRepository.existsByTeacherIdAndPhoneNumberIgnoreCase(teacherId, phoneNumber)) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_DUPLICATE_PHONE);
        }
    }

    private StudentProfile getProfileForTeacher(UUID teacherId, UUID profileId) {
        return studentProfileRepository.findByIdAndTeacherId(profileId, teacherId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
    }

    private Member getAssistant(UUID assistantId, UUID teacherId) {
        Member assistant = memberRepository.findById(assistantId)
                .orElseThrow(() -> new BusinessException(RsCode.ASSISTANT_NOT_FOUND));
        if (assistant.getRole() != MemberRole.ASSISTANT || !teacherId.equals(assistant.getTeacherId())) {
            throw new BusinessException(RsCode.ASSISTANT_NOT_FOUND);
        }
        return assistant;
    }

    private Member getStudentMember(UUID memberId) {
        Member student = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));
        if (student.getRole() != MemberRole.STUDENT) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return student;
    }

    private void ensureMemberNotLinked(UUID memberId) {
        if (studentProfileRepository.existsByMemberId(memberId)) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_MEMBER_IN_USE);
        }
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));
    }

    private void ensureTeacher(Member member) {
        if (member.getRole() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private UUID resolveTeacherId(Member actor) {
        if (actor.getRole() == MemberRole.TEACHER) {
            return actor.getId();
        }
        if (actor.getRole() == MemberRole.ASSISTANT && actor.getTeacherId() != null) {
            return actor.getTeacherId();
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private Page<StudentProfileSummary> enrichPageWithNames(Page<StudentProfile> page) {
        List<StudentProfile> profiles = page.getContent();
        List<StudentProfileSummary> enriched = enrichListWithNames(profiles);
        Map<UUID, StudentProfileSummary> summaryMap = enriched.stream()
                .collect(Collectors.toMap(StudentProfileSummary::id, summary -> summary));
        return page.map(profile -> summaryMap.getOrDefault(
                profile.getId(),
                StudentProfileSummary.of(profile, "Unknown", List.of())
        ));
    }

    private List<StudentProfileSummary> enrichListWithNames(List<StudentProfile> profiles) {
        if (profiles.isEmpty()) {
            return List.of();
        }

        Set<UUID> assistantIds = profiles.stream()
                .map(StudentProfile::getAssistantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> assistantNames = memberRepository.findAllById(assistantIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));

        Set<UUID> profileIds = profiles.stream()
                .map(StudentProfile::getId)
                .collect(Collectors.toSet());

        List<StudentCourseEnrollment> enrollments =
                studentCourseEnrollmentRepository.findAllByStudentProfileIdIn(profileIds);
        Map<UUID, List<StudentCourseEnrollment>> enrollmentsByProfile = enrollments.stream()
                .collect(Collectors.groupingBy(StudentCourseEnrollment::getStudentProfileId));

        Set<UUID> courseIds = enrollments.stream()
                .map(StudentCourseEnrollment::getCourseId)
                .collect(Collectors.toSet());

        Map<UUID, String> courseNames = courseRepository.findAllById(courseIds)
                .stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));

        return profiles.stream()
                .map(profile -> {
                    List<String> courseNameList = enrollmentsByProfile
                            .getOrDefault(profile.getId(), List.of())
                            .stream()
                            .map(enrollment -> courseNames.getOrDefault(enrollment.getCourseId(), "Unknown"))
                            .toList();
                    return StudentProfileSummary.of(
                            profile,
                            profile.getAssistantId() != null
                                    ? assistantNames.getOrDefault(profile.getAssistantId(), "Unknown")
                                    : "미배정",
                            courseNameList
                    );
                })
                .toList();
    }
}
