package com.classhub.domain.studentprofile.application;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileSearchCondition;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.dto.response.StudentProfileSummary;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Map;
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
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public StudentProfileResponse createProfile(UUID principalId, StudentProfileCreateRequest request) {
        Member actor = getMember(principalId);
        ensureTeacher(actor);

        Course course = getCourseOwnedByTeacher(request.courseId(), actor.getId());
        validateDuplicatePhoneNumber(actor.getId(), course.getId(), request.normalizedPhoneNumber());

        Member assistant = getAssistant(request.assistantId(), actor.getId());

        StudentProfile profile = StudentProfile.builder()
                .courseId(course.getId())
                .teacherId(actor.getId())
                .assistantId(assistant.getId())
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
        return StudentProfileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(UUID principalId, UUID profileId) {
        Member actor = getMember(principalId);
        UUID teacherId = resolveTeacherId(actor);
        StudentProfile profile = getProfileForTeacher(teacherId, profileId);
        return StudentProfileResponse.from(profile);
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
            if (condition.hasNameFilter()) {
                page = active == null
                        ? studentProfileRepository.findAllByTeacherIdAndCourseIdAndNameContainingIgnoreCase(
                        teacherId,
                        condition.courseId(),
                        condition.name(),
                        pageable
                )
                        : studentProfileRepository.findAllByTeacherIdAndCourseIdAndActiveAndNameContainingIgnoreCase(
                                teacherId,
                                condition.courseId(),
                                active,
                                condition.name(),
                                pageable
                        );
            } else {
                page = active == null
                        ? studentProfileRepository.findAllByTeacherIdAndCourseId(teacherId, condition.courseId(), pageable)
                        : studentProfileRepository.findAllByTeacherIdAndCourseIdAndActive(
                                teacherId, condition.courseId(), active, pageable
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

        if (request.assistantId() != null && !request.assistantId().equals(profile.getAssistantId())) {
            Member assistant = getAssistant(request.assistantId(), actor.getId());
            profile.assignAssistant(assistant.getId());
        }

        UUID targetCourseId = profile.getCourseId();
        boolean courseChangeRequested = request.courseId() != null
                && !request.courseId().equals(profile.getCourseId());
        if (courseChangeRequested) {
            Course newCourse = getCourseOwnedByTeacher(request.courseId(), actor.getId());
            targetCourseId = newCourse.getId();
        }

        boolean phoneChangeRequested = request.phoneNumber() != null;
        String normalizedPhone = profile.getPhoneNumber();
        if (phoneChangeRequested) {
            normalizedPhone = request.phoneNumber().trim();
            phoneChangeRequested = !normalizedPhone.equals(profile.getPhoneNumber());
        }

        if (courseChangeRequested || phoneChangeRequested) {
            validateDuplicatePhoneNumber(actor.getId(), targetCourseId, normalizedPhone);
        }

        if (courseChangeRequested) {
            profile.moveToCourse(targetCourseId);
        }
        if (phoneChangeRequested) {
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
        return StudentProfileResponse.from(saved);
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
        List<StudentProfile> profiles = studentProfileRepository
                .findAllByTeacherIdAndCourseIdAndActive(teacherId, courseId, true);
        return enrichListWithNames(profiles);
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

    private void validateDuplicatePhoneNumber(UUID teacherId, UUID courseId, String phoneNumber) {
        if (studentProfileRepository.existsByTeacherIdAndCourseIdAndPhoneNumberIgnoreCase(
                teacherId,
                courseId,
                phoneNumber
        )) {
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
        return page.map(profile -> {
            return enriched.stream()
                    .filter(summary -> summary.id().equals(profile.getId()))
                    .findFirst()
                    .orElseThrow();
        });
    }

    private List<StudentProfileSummary> enrichListWithNames(List<StudentProfile> profiles) {
        if (profiles.isEmpty()) {
            return List.of();
        }

        Set<UUID> assistantIds = profiles.stream()
                .map(StudentProfile::getAssistantId)
                .collect(Collectors.toSet());
        Set<UUID> courseIds = profiles.stream()
                .map(StudentProfile::getCourseId)
                .collect(Collectors.toSet());

        Map<UUID, String> assistantNames = memberRepository.findAllById(assistantIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));

        Map<UUID, String> courseNames = courseRepository.findAllById(courseIds)
                .stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));

        return profiles.stream()
                .map(profile -> StudentProfileSummary.from(
                        profile,
                        assistantNames.getOrDefault(profile.getAssistantId(), "Unknown"),
                        courseNames.getOrDefault(profile.getCourseId(), "Unknown")
                ))
                .toList();
    }
}
