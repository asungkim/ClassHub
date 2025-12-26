package com.classhub.domain.studentcourse.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.slot.application.ClinicDefaultSlotService;
import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.dto.StudentCourseStatusFilter;
import com.classhub.domain.studentcourse.dto.request.StudentCourseRecordUpdateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseRecordSummaryResponse;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse;
import com.classhub.domain.studentcourse.dto.response.StudentCourseListItemResponse;
import com.classhub.domain.studentcourse.dto.response.StudentStudentDetailResponse;
import com.classhub.domain.studentcourse.dto.response.StudentStudentListItemResponse;
import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.domain.studentcourse.repository.StudentActiveCourseProjection;
import com.classhub.domain.studentcourse.repository.StudentStatusProjection;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
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
public class StudentCourseManagementService {

    private final StudentCourseRecordRepository recordRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final TeacherAssistantAssignmentRepository assignmentRepository;
    private final CourseViewAssembler courseViewAssembler;
    private final ClinicDefaultSlotService clinicDefaultSlotService;

    public PageResponse<StudentCourseListItemResponse> getStudentCourses(MemberPrincipal principal,
                                                                         UUID courseId,
                                                                         StudentCourseStatusFilter statusFilter,
                                                                         String keyword,
                                                                         int page,
                                                                         int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        boolean activeOnly = statusFilter == StudentCourseStatusFilter.ACTIVE;
        boolean inactiveOnly = statusFilter == StudentCourseStatusFilter.INACTIVE;
        Page<StudentCourseRecord> recordPage;
        if (principal.role() == MemberRole.TEACHER) {
            recordPage = recordRepository.searchRecordsForTeacher(
                    principal.id(),
                    courseId,
                    activeOnly,
                    inactiveOnly,
                    normalizedKeyword,
                    pageable);
        } else if (principal.role() == MemberRole.ASSISTANT) {
            List<TeacherAssistantAssignment> assignments = assignmentRepository
                    .findByAssistantMemberIdAndDeletedAtIsNull(principal.id());
            if (assignments.isEmpty()) {
                return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
            }
            List<UUID> teacherIds = assignments.stream()
                    .map(TeacherAssistantAssignment::getTeacherMemberId)
                    .distinct()
                    .toList();
            recordPage = recordRepository.searchRecordsForTeachers(
                    teacherIds,
                    courseId,
                    activeOnly,
                    inactiveOnly,
                    normalizedKeyword,
                    pageable);
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (recordPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, recordPage.getTotalElements()));
        }
        Map<UUID, Member> memberMap = loadMembers(recordPage.getContent());
        Map<UUID, StudentInfo> infoMap = loadStudentInfos(recordPage.getContent());
        Map<UUID, Course> courseMap = loadCourses(recordPage.getContent());
        List<StudentCourseListItemResponse> content = recordPage.getContent().stream()
                .map(record -> toListItem(record, memberMap, infoMap, courseMap))
                .toList();
        Page<StudentCourseListItemResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                recordPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    public PageResponse<StudentStudentListItemResponse> getStudents(MemberPrincipal principal,
                                                                    StudentCourseStatusFilter statusFilter,
                                                                    String keyword,
                                                                    int page,
                                                                    int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        boolean activeOnly = statusFilter == StudentCourseStatusFilter.ACTIVE;
        boolean inactiveOnly = statusFilter == StudentCourseStatusFilter.INACTIVE;
        List<UUID> teacherIds;
        Page<StudentStatusProjection> studentPage;
        if (principal.role() == MemberRole.TEACHER) {
            teacherIds = List.of(principal.id());
            studentPage = enrollmentRepository.searchStudentSummariesForTeacher(
                    principal.id(),
                    activeOnly,
                    inactiveOnly,
                    normalizedKeyword,
                    pageable
            );
        } else if (principal.role() == MemberRole.ASSISTANT) {
            List<TeacherAssistantAssignment> assignments = assignmentRepository
                    .findByAssistantMemberIdAndDeletedAtIsNull(principal.id());
            if (assignments.isEmpty()) {
                return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
            }
            teacherIds = assignments.stream()
                    .map(TeacherAssistantAssignment::getTeacherMemberId)
                    .distinct()
                    .toList();
            studentPage = enrollmentRepository.searchStudentSummariesForTeachers(
                    teacherIds,
                    activeOnly,
                    inactiveOnly,
                    normalizedKeyword,
                    pageable
            );
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (studentPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, studentPage.getTotalElements()));
        }
        List<UUID> studentIds = studentPage.getContent().stream()
                .map(StudentStatusProjection::getStudentMemberId)
                .distinct()
                .toList();
        Map<UUID, ActiveCourseSummary> activeCourseMap = loadActiveCourseSummary(teacherIds, studentIds);
        Map<UUID, Member> memberMap = loadMembersByIds(studentIds);
        Map<UUID, StudentInfo> infoMap = loadStudentInfosByIds(studentIds);
        List<StudentStudentListItemResponse> content = studentPage.getContent().stream()
                .map(projection -> {
                    UUID studentId = projection.getStudentMemberId();
                    Member member = memberMap.get(studentId);
                    StudentInfo info = infoMap.get(studentId);
                    if (member == null || info == null) {
                        throw new BusinessException(RsCode.INTERNAL_SERVER);
                    }
                    ActiveCourseSummary summary = activeCourseMap.getOrDefault(studentId, ActiveCourseSummary.empty());
                    return new StudentStudentListItemResponse(
                            toStudentSummary(member, info),
                            projection.getActive(),
                            summary.courseIds(),
                            summary.courseNames()
                    );
                })
                .toList();
        Page<StudentStudentListItemResponse> dtoPage = new PageImpl<>(
                content,
                pageable,
                studentPage.getTotalElements()
        );
        return PageResponse.from(dtoPage);
    }

    public StudentStudentDetailResponse getStudentDetail(MemberPrincipal principal, UUID studentId) {
        List<UUID> teacherIds = resolveTeacherIds(principal);
        List<StudentCourseEnrollment> enrollments = enrollmentRepository
                .findByStudentMemberIdAndTeacherMemberIdIn(studentId, teacherIds);
        if (enrollments.isEmpty()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        List<StudentCourseRecord> records = recordRepository
                .findByStudentMemberIdAndTeacherMemberIdIn(studentId, teacherIds);
        StudentSummaryResponse studentSummary = loadStudentSummary(studentId);
        List<CourseResponse> courseResponses = loadCourseResponses(enrollments, records);
        List<StudentCourseRecordSummaryResponse> recordResponses = records.stream()
                .map(record -> new StudentCourseRecordSummaryResponse(
                        record.getId(),
                        record.getCourseId(),
                        record.getAssistantMemberId(),
                        record.getDefaultClinicSlotId(),
                        record.getTeacherNotes(),
                        !record.isDeleted()
                ))
                .toList();
        return new StudentStudentDetailResponse(studentSummary, courseResponses, recordResponses);
    }

    public StudentCourseDetailResponse getStudentCourseDetail(UUID teacherId, UUID recordId) {
        StudentCourseRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND));
        Course course = courseRepository.findById(record.getCourseId())
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        ensureTeacher(course, teacherId);
        return buildDetailResponse(record, course);
    }

    @Transactional
    public StudentCourseDetailResponse updateStudentCourseRecord(UUID teacherId,
                                                                 UUID recordId,
                                                                 StudentCourseRecordUpdateRequest request) {
        StudentCourseRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND));
        Course course = courseRepository.findById(record.getCourseId())
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        ensureTeacher(course, teacherId);
        if (request.assistantMemberId() != null) {
            boolean assigned = assignmentRepository
                    .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, request.assistantMemberId())
                    .isPresent();
            if (!assigned) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            record.updateAssistant(request.assistantMemberId());
        }
        if (request.defaultClinicSlotId() != null) {
            clinicDefaultSlotService.applyDefaultSlot(record, course, request.defaultClinicSlotId());
        }
        if (request.teacherNotes() != null) {
            record.updateTeacherNotes(request.teacherNotes());
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(course));
        CourseResponse courseResponse = courseViewAssembler.toCourseResponse(course, context);
        StudentSummaryResponse studentSummary = loadStudentSummary(record.getStudentMemberId());
        return new StudentCourseDetailResponse(
                record.getId(),
                studentSummary,
                courseResponse,
                record.getAssistantMemberId(),
                record.getDefaultClinicSlotId(),
                record.getTeacherNotes(),
                !record.isDeleted());
    }

    private StudentCourseDetailResponse buildDetailResponse(StudentCourseRecord record, Course course) {
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(course));
        CourseResponse courseResponse = courseViewAssembler.toCourseResponse(course, context);
        StudentSummaryResponse studentSummary = loadStudentSummary(record.getStudentMemberId());
        return new StudentCourseDetailResponse(
                record.getId(),
                studentSummary,
                courseResponse,
                record.getAssistantMemberId(),
                record.getDefaultClinicSlotId(),
                record.getTeacherNotes(),
                !record.isDeleted());
    }

    private StudentSummaryResponse loadStudentSummary(UUID studentId) {
        Member member = memberRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));
        StudentInfo info = studentInfoRepository.findByMemberId(studentId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        return toStudentSummary(member, info);
    }

    private StudentCourseListItemResponse toListItem(StudentCourseRecord record,
                                                     Map<UUID, Member> memberMap,
                                                     Map<UUID, StudentInfo> infoMap,
                                                     Map<UUID, Course> courseMap) {
        Member member = memberMap.get(record.getStudentMemberId());
        StudentInfo info = infoMap.get(record.getStudentMemberId());
        Course course = courseMap.get(record.getCourseId());
        if (member == null || info == null || course == null) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return new StudentCourseListItemResponse(
                record.getId(),
                member.getId(),
                member.getName(),
                member.getPhoneNumber(),
                info.getParentPhone(),
                info.getSchoolName(),
                info.getGrade().name(),
                calculateAge(info.getBirthDate()),
                course.getId(),
                course.getName(),
                !record.isDeleted(),
                record.getAssistantMemberId(),
                record.getDefaultClinicSlotId());
    }

    private Map<UUID, Member> loadMembers(Collection<StudentCourseRecord> records) {
        List<UUID> studentIds = records.stream()
                .map(StudentCourseRecord::getStudentMemberId)
                .distinct()
                .toList();
        return loadMembersByIds(studentIds);
    }

    private Map<UUID, StudentInfo> loadStudentInfos(Collection<StudentCourseRecord> records) {
        List<UUID> studentIds = records.stream()
                .map(StudentCourseRecord::getStudentMemberId)
                .distinct()
                .toList();
        return loadStudentInfosByIds(studentIds);
    }

    private Map<UUID, Member> loadMembersByIds(Collection<UUID> studentIds) {
        List<Member> members = memberRepository.findAllById(studentIds);
        if (members.size() < studentIds.size()) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return members.stream().collect(Collectors.toMap(Member::getId, member -> member));
    }

    private Map<UUID, StudentInfo> loadStudentInfosByIds(Collection<UUID> studentIds) {
        List<StudentInfo> infos = studentInfoRepository.findByMemberIdIn(studentIds);
        if (infos.size() < studentIds.size()) {
            throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
        }
        return infos.stream().collect(Collectors.toMap(StudentInfo::getMemberId, info -> info));
    }

    private Map<UUID, Course> loadCourses(Collection<StudentCourseRecord> records) {
        List<UUID> courseIds = records.stream()
                .map(StudentCourseRecord::getCourseId)
                .distinct()
                .toList();
        List<Course> courses = courseRepository.findAllById(courseIds);
        if (courses.size() < courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        return courses.stream().collect(Collectors.toMap(Course::getId, course -> course));
    }

    private void ensureTeacher(Course course, UUID teacherId) {
        if (!course.getTeacherMemberId().equals(teacherId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    private List<CourseResponse> loadCourseResponses(List<StudentCourseEnrollment> enrollments,
                                                     List<StudentCourseRecord> records) {
        HashSet<UUID> courseIds = new HashSet<>();
        enrollments.forEach(enrollment -> courseIds.add(enrollment.getCourseId()));
        records.forEach(record -> courseIds.add(record.getCourseId()));
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<Course> courses = courseRepository.findAllById(courseIds);
        if (courses.size() < courseIds.size()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        return courses.stream()
                .map(course -> courseViewAssembler.toCourseResponse(course, context))
                .sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
                .toList();
    }

    private List<UUID> resolveTeacherIds(MemberPrincipal principal) {
        if (principal.role() == MemberRole.TEACHER) {
            return List.of(principal.id());
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            List<TeacherAssistantAssignment> assignments = assignmentRepository
                    .findByAssistantMemberIdAndDeletedAtIsNull(principal.id());
            if (assignments.isEmpty()) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return assignments.stream()
                    .map(TeacherAssistantAssignment::getTeacherMemberId)
                    .distinct()
                    .toList();
        }
        throw new BusinessException(RsCode.FORBIDDEN);
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

    private Map<UUID, ActiveCourseSummary> loadActiveCourseSummary(List<UUID> teacherIds, List<UUID> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        List<StudentActiveCourseProjection> projections = enrollmentRepository
                .findActiveCoursesForStudents(teacherIds, studentIds);
        Map<UUID, ActiveCourseSummaryBuilder> builderMap = new java.util.HashMap<>();
        for (StudentActiveCourseProjection projection : projections) {
            ActiveCourseSummaryBuilder builder = builderMap.computeIfAbsent(
                    projection.getStudentMemberId(),
                    key -> new ActiveCourseSummaryBuilder()
            );
            builder.add(projection.getCourseId(), projection.getCourseName());
        }
        return builderMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().build()
                ));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ActiveCourseSummary(List<UUID> courseIds, List<String> courseNames) {
        private static ActiveCourseSummary empty() {
            return new ActiveCourseSummary(List.of(), List.of());
        }
    }

    private static class ActiveCourseSummaryBuilder {
        private final List<UUID> courseIds = new java.util.ArrayList<>();
        private final List<String> courseNames = new java.util.ArrayList<>();

        private void add(UUID courseId, String courseName) {
            courseIds.add(courseId);
            courseNames.add(courseName);
        }

        private ActiveCourseSummary build() {
            return new ActiveCourseSummary(List.copyOf(courseIds), List.copyOf(courseNames));
        }
    }
}
