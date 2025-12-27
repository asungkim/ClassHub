package com.classhub.domain.assignment.application;

import com.classhub.domain.assignment.dto.response.TeacherStudentCourseResponse;
import com.classhub.domain.assignment.dto.response.TeacherStudentDetailResponse;
import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherStudentService {

    private final TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;
    private final TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    private final StudentCourseAssignmentRepository studentCourseAssignmentRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;

    public PageResponse<StudentSummaryResponse> getTeacherStudents(MemberPrincipal principal,
                                                                   UUID courseId,
                                                                   String keyword,
                                                                   int page,
                                                                   int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (principal.role() == MemberRole.TEACHER) {
            Page<TeacherStudentAssignment> assignmentPage = teacherStudentAssignmentRepository.searchAssignmentsForTeacherByCourse(
                    principal.id(),
                    courseId,
                    normalizedKeyword,
                    pageable
            );
            return toStudentSummaryResponseFromAssignments(assignmentPage, pageable);
        } else if (principal.role() == MemberRole.ASSISTANT) {
            List<UUID> teacherIds = resolveAssistantTeacherIds(principal.id());
            if (teacherIds.isEmpty()) {
                return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
            }
            Page<UUID> studentIdPage = teacherStudentAssignmentRepository.searchDistinctStudentIdsForTeachers(
                    teacherIds,
                    courseId,
                    normalizedKeyword,
                    pageable
            );
            return toStudentSummaryResponseFromStudentIds(studentIdPage, pageable);
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private PageResponse<StudentSummaryResponse> toStudentSummaryResponseFromAssignments(
            Page<TeacherStudentAssignment> assignmentPage,
            PageRequest pageable
    ) {
        if (assignmentPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, assignmentPage.getTotalElements()));
        }
        List<UUID> studentIds = assignmentPage.getContent().stream()
                .map(TeacherStudentAssignment::getStudentMemberId)
                .distinct()
                .toList();
        Map<UUID, Member> memberMap = loadMembers(studentIds);
        Map<UUID, StudentInfo> infoMap = loadStudentInfos(studentIds);
        List<StudentSummaryResponse> content = assignmentPage.getContent().stream()
                .map(assignment -> toStudentSummary(memberMap, infoMap, assignment.getStudentMemberId()))
                .toList();
        Page<StudentSummaryResponse> dtoPage = new PageImpl<>(content, pageable, assignmentPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    private PageResponse<StudentSummaryResponse> toStudentSummaryResponseFromStudentIds(
            Page<UUID> studentIdPage,
            PageRequest pageable
    ) {
        if (studentIdPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, studentIdPage.getTotalElements()));
        }
        List<UUID> studentIds = studentIdPage.getContent();
        Map<UUID, Member> memberMap = loadMembers(studentIds);
        Map<UUID, StudentInfo> infoMap = loadStudentInfos(studentIds);
        List<StudentSummaryResponse> content = studentIds.stream()
                .map(studentId -> toStudentSummary(memberMap, infoMap, studentId))
                .toList();
        Page<StudentSummaryResponse> dtoPage = new PageImpl<>(content, pageable, studentIdPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    private StudentSummaryResponse toStudentSummary(Map<UUID, Member> memberMap,
                                                    Map<UUID, StudentInfo> infoMap,
                                                    UUID studentId) {
        Member member = memberMap.get(studentId);
        StudentInfo info = infoMap.get(studentId);
        if (member == null || info == null) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return toStudentSummary(member, info);
    }

    public TeacherStudentDetailResponse getTeacherStudentDetail(MemberPrincipal principal, UUID studentId) {
        List<UUID> teacherIds = resolveTeacherIds(principal);
        if (teacherIds.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        boolean assigned = principal.role() == MemberRole.TEACHER
                ? teacherStudentAssignmentRepository
                        .existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(principal.id(), studentId)
                : teacherStudentAssignmentRepository
                        .existsByTeacherMemberIdInAndStudentMemberIdAndDeletedAtIsNull(teacherIds, studentId);
        if (!assigned) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));
        StudentInfo info = studentInfoRepository.findByMemberId(studentId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        StudentSummaryResponse studentSummary = toStudentSummary(member, info);

        List<StudentCourseAssignment> assignments = studentCourseAssignmentRepository.findByStudentMemberId(studentId);
        Map<UUID, StudentCourseAssignment> assignmentMap = assignments.stream()
                .collect(Collectors.toMap(StudentCourseAssignment::getCourseId, assignment -> assignment, (a, b) -> a));

        List<StudentCourseRecord> records = studentCourseRecordRepository
                .findByStudentMemberIdAndTeacherMemberIdIn(studentId, teacherIds);
        Map<UUID, StudentCourseRecord> recordMap = records.stream()
                .collect(Collectors.toMap(StudentCourseRecord::getCourseId, record -> record, (a, b) -> a));

        Set<UUID> courseIds = new HashSet<>();
        assignmentMap.keySet().forEach(courseIds::add);
        recordMap.keySet().forEach(courseIds::add);
        if (courseIds.isEmpty()) {
            return new TeacherStudentDetailResponse(studentSummary, List.of());
        }
        Map<UUID, Course> courseMap = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, course -> course));
        if (courseMap.size() < courseIds.size()) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        List<Course> filteredCourses = courseMap.values().stream()
                .filter(course -> teacherIds.contains(course.getTeacherMemberId()))
                .sorted(courseComparator())
                .toList();
        List<TeacherStudentCourseResponse> courseResponses = filteredCourses.stream()
                .map(course -> toCourseResponse(course, assignmentMap.get(course.getId()), recordMap.get(course.getId())))
                .toList();
        return new TeacherStudentDetailResponse(studentSummary, courseResponses);
    }

    private TeacherStudentCourseResponse toCourseResponse(Course course,
                                                          StudentCourseAssignment assignment,
                                                          StudentCourseRecord record) {
        return new TeacherStudentCourseResponse(
                course.getId(),
                course.getName(),
                course.getStartDate(),
                course.getEndDate(),
                !course.isDeleted(),
                assignment == null ? null : assignment.getId(),
                assignment == null ? null : assignment.isActive(),
                record == null ? null : record.getId()
        );
    }

    private Comparator<Course> courseComparator() {
        return Comparator
                .comparing(Course::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Map<UUID, Member> loadMembers(List<UUID> studentIds) {
        Map<UUID, Member> memberMap = memberRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        if (memberMap.size() < studentIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return memberMap;
    }

    private Map<UUID, StudentInfo> loadStudentInfos(List<UUID> studentIds) {
        Map<UUID, StudentInfo> infoMap = studentInfoRepository.findByMemberIdIn(studentIds).stream()
                .collect(Collectors.toMap(StudentInfo::getMemberId, info -> info));
        if (infoMap.size() < studentIds.size()) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
        return infoMap;
    }

    private StudentSummaryResponse toStudentSummary(Member member, StudentInfo info) {
        return StudentSummaryResponse.builder()
                .memberId(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .schoolName(info.getSchoolName())
                .grade(info.getGrade().name())
                .birthDate(info.getBirthDate())
                .age(calculateAge(info.getBirthDate()))
                .parentPhone(info.getParentPhone())
                .build();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now(KstTime.clock())).getYears();
    }

    private List<UUID> resolveTeacherIds(MemberPrincipal principal) {
        if (principal.role() == MemberRole.TEACHER) {
            return List.of(principal.id());
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            return resolveAssistantTeacherIds(principal.id());
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private List<UUID> resolveAssistantTeacherIds(UUID assistantId) {
        List<TeacherAssistantAssignment> assignments = teacherAssistantAssignmentRepository
                .findByAssistantMemberIdAndDeletedAtIsNull(assistantId);
        if (assignments.isEmpty()) {
            return List.of();
        }
        return assignments.stream()
                .map(TeacherAssistantAssignment::getTeacherMemberId)
                .distinct()
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
