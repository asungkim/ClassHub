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
import java.util.UUID;
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
    public StudentProfileResponse createProfile(UUID teacherId, StudentProfileCreateRequest request) {
        Course course = getCourseOwnedByTeacher(request.courseId(), teacherId);
        validateDuplicatePhoneNumber(teacherId, course.getId(), request.normalizedPhoneNumber());

        Member assistant = getAssistant(request.assistantId(), teacherId);

        UUID memberId = null;
        if (request.memberId() != null) {
            Member student = getStudentMember(request.memberId());
            ensureMemberNotLinked(student.getId());
            memberId = student.getId();
        }

        StudentProfile profile = StudentProfile.builder()
                .courseId(course.getId())
                .teacherId(course.getTeacherId())
                .assistantId(assistant.getId())
                .memberId(memberId)
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
    public StudentProfileResponse getProfile(UUID teacherId, UUID profileId) {
        StudentProfile profile = getProfileForTeacher(teacherId, profileId);
        return StudentProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public Page<StudentProfileSummary> getProfiles(
            UUID teacherId,
            StudentProfileSearchCondition condition,
            Pageable pageable
    ) {
        Page<StudentProfile> page;
        if (condition != null && condition.hasCourseFilter()) {
            ensureCourseOwnership(condition.courseId(), teacherId);
            if (condition.hasNameFilter()) {
                page = studentProfileRepository
                        .findAllByTeacherIdAndCourseIdAndActiveTrueAndNameContainingIgnoreCase(
                                teacherId,
                                condition.courseId(),
                                condition.name(),
                                pageable
                        );
            } else {
                page = studentProfileRepository
                        .findAllByTeacherIdAndCourseIdAndActiveTrue(teacherId, condition.courseId(), pageable);
            }
        } else if (condition != null && condition.hasNameFilter()) {
            page = studentProfileRepository
                    .findAllByTeacherIdAndActiveTrueAndNameContainingIgnoreCase(
                            teacherId,
                            condition.name(),
                            pageable
                    );
        } else {
            page = studentProfileRepository.findAllByTeacherIdAndActiveTrue(teacherId, pageable);
        }

        return page.map(StudentProfileSummary::from);
    }

    @Transactional
    public StudentProfileResponse updateProfile(
            UUID teacherId,
            UUID profileId,
            StudentProfileUpdateRequest request
    ) {
        StudentProfile profile = getProfileForTeacher(teacherId, profileId);

        if (request.assistantId() != null && !request.assistantId().equals(profile.getAssistantId())) {

            Member assistant = getAssistant(request.assistantId(), teacherId);
            profile.assignAssistant(assistant.getId());
        }

        if (request.phoneNumber() != null
                && !request.phoneNumber().equals(profile.getPhoneNumber())) {
            String normalized = request.phoneNumber().trim();
            validateDuplicatePhoneNumber(teacherId, profile.getCourseId(), normalized);
            profile.changePhoneNumber(normalized);
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
    public void deleteProfile(UUID teacherId, UUID profileId) {
        StudentProfile profile = getProfileForTeacher(teacherId, profileId);
        profile.deactivate();
        studentProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<StudentProfileSummary> getCourseStudents(UUID teacherId, UUID courseId) {
        ensureCourseOwnership(courseId, teacherId);
        return studentProfileRepository.findAllByTeacherIdAndCourseIdAndActiveTrue(teacherId, courseId)
                .stream()
                .map(StudentProfileSummary::from)
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
}
