package com.classhub.domain.course.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherStudentAssignmentRepository;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.slot.application.ClinicDefaultSlotService;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseStudentResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.dto.request.StudentCourseAssignmentCreateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseAssignmentResponse;
import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseAssignmentRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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
public class CourseAssignmentService {

    private final CourseRepository courseRepository;
    private final TeacherAssistantAssignmentRepository assistantAssignmentRepository;
    private final TeacherStudentAssignmentRepository teacherStudentAssignmentRepository;
    private final StudentCourseAssignmentRepository studentCourseAssignmentRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final CourseViewAssembler courseViewAssembler;
    private final ClinicAttendanceRepository clinicAttendanceRepository;
    private final ClinicDefaultSlotService clinicDefaultSlotService;

    public PageResponse<CourseResponse> getAssignableCourses(MemberPrincipal principal,
                                                             UUID branchId,
                                                             String keyword,
                                                             int page,
                                                             int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        LocalDate today = LocalDate.now(KstTime.clock());
        Page<Course> coursePage;
        if (principal.role() == MemberRole.TEACHER) {
            coursePage = courseRepository.searchAssignableCoursesForTeacher(
                    principal.id(),
                    branchId,
                    normalizedKeyword,
                    today,
                    pageable);
        } else if (principal.role() == MemberRole.ASSISTANT) {
            List<TeacherAssistantAssignment> assignments = assistantAssignmentRepository
                    .findByAssistantMemberIdAndDeletedAtIsNull(principal.id());
            if (assignments.isEmpty()) {
                return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
            }
            List<UUID> teacherIds = assignments.stream()
                    .map(TeacherAssistantAssignment::getTeacherMemberId)
                    .distinct()
                    .toList();
            coursePage = courseRepository.searchAssignableCoursesForTeachers(
                    teacherIds,
                    branchId,
                    normalizedKeyword,
                    today,
                    pageable);
        } else {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (coursePage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(coursePage.getContent());
        Page<CourseResponse> responsePage = coursePage.map(course ->
                courseViewAssembler.toCourseResponse(course, context));
        return PageResponse.from(responsePage);
    }

    public PageResponse<StudentSummaryResponse> getAssignmentCandidates(MemberPrincipal principal,
                                                                        UUID courseId,
                                                                        String keyword,
                                                                        int page,
                                                                        int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Course course = loadCourse(courseId);
        ensurePermission(principal, course.getTeacherMemberId());

        List<UUID> excludeIds = studentCourseAssignmentRepository.findStudentMemberIdsByCourseId(courseId);
        List<UUID> effectiveExcludes = excludeIds.isEmpty() ? null : excludeIds;
        Page<TeacherStudentAssignment> assignmentPage = teacherStudentAssignmentRepository
                .searchAssignmentsForTeacher(course.getTeacherMemberId(), normalizeKeyword(keyword), effectiveExcludes, pageable);

        if (assignmentPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }
        List<UUID> studentIds = assignmentPage.getContent().stream()
                .map(TeacherStudentAssignment::getStudentMemberId)
                .distinct()
                .toList();
        Map<UUID, Member> memberMap = loadMembers(studentIds);
        Map<UUID, StudentInfo> infoMap = loadStudentInfos(studentIds);
        List<StudentSummaryResponse> content = assignmentPage.getContent().stream()
                .map(assignment -> {
                    Member member = memberMap.get(assignment.getStudentMemberId());
                    StudentInfo info = infoMap.get(assignment.getStudentMemberId());
                    if (member == null || info == null) {
                        throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
                    }
                    return toStudentSummary(member, info);
                })
                .toList();
        Page<StudentSummaryResponse> dtoPage = new PageImpl<>(content, pageable, assignmentPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    public PageResponse<CourseStudentResponse> getCourseStudents(MemberPrincipal principal,
                                                                 UUID courseId,
                                                                 int page,
                                                                 int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Course course = loadCourse(courseId);
        ensurePermission(principal, course.getTeacherMemberId());

        Page<StudentCourseAssignment> assignmentPage = studentCourseAssignmentRepository
                .findByCourseId(courseId, pageable);
        if (assignmentPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }

        List<StudentCourseAssignment> assignments = assignmentPage.getContent();
        List<UUID> studentIds = assignments.stream()
                .map(StudentCourseAssignment::getStudentMemberId)
                .distinct()
                .toList();
        Map<UUID, Member> memberMap = loadMembers(studentIds);
        Map<UUID, StudentInfo> infoMap = loadStudentInfos(studentIds);
        Map<UUID, StudentCourseRecord> recordMap = studentCourseRecordRepository
                .findActiveByCourseIdAndStudentIds(courseId, studentIds)
                .stream()
                .collect(Collectors.toMap(
                        StudentCourseRecord::getStudentMemberId,
                        record -> record,
                        (a, b) -> a
                ));

        List<CourseStudentResponse> content = assignments.stream()
                .map(assignment -> {
                    UUID studentId = assignment.getStudentMemberId();
                    Member member = memberMap.get(studentId);
                    StudentInfo info = infoMap.get(studentId);
                    if (member == null || info == null) {
                        throw new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND);
                    }
                    StudentCourseRecord record = recordMap.get(studentId);
                    if (record == null) {
                        throw new BusinessException(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND);
                    }
                    return new CourseStudentResponse(
                            record.getId(),
                            assignment.isActive(),
                            toStudentSummary(member, info)
                    );
                })
                .toList();

        Page<CourseStudentResponse> dtoPage = new PageImpl<>(content, pageable, assignmentPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public StudentCourseAssignmentResponse createAssignment(MemberPrincipal principal,
                                                            StudentCourseAssignmentCreateRequest request) {
        Course course = loadCourse(request.courseId());
        ensurePermission(principal, course.getTeacherMemberId());
        boolean linked = teacherStudentAssignmentRepository
                .existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(course.getTeacherMemberId(), request.studentId());
        if (!linked) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        boolean exists = studentCourseAssignmentRepository
                .existsByStudentMemberIdAndCourseId(request.studentId(), request.courseId());
        if (exists) {
            throw new BusinessException(RsCode.STUDENT_COURSE_ASSIGNMENT_ALREADY_EXISTS);
        }
        StudentCourseAssignment assignment = StudentCourseAssignment.create(
                request.studentId(),
                request.courseId(),
                principal.id(),
                LocalDateTime.now(KstTime.clock()));
        StudentCourseAssignment saved = studentCourseAssignmentRepository.save(assignment);

        studentCourseRecordRepository
                .findByStudentMemberIdAndCourseIdAndDeletedAtIsNull(request.studentId(), request.courseId())
                .orElseGet(() -> studentCourseRecordRepository.save(
                        StudentCourseRecord.create(request.studentId(), request.courseId(), null, null, null)
                ));

        return StudentCourseAssignmentResponse.from(saved);
    }

    @Transactional
    public StudentCourseAssignmentResponse activateAssignment(MemberPrincipal principal, UUID assignmentId) {
        StudentCourseAssignment assignment = loadAssignment(assignmentId);
        Course course = loadCourse(assignment.getCourseId());
        ensurePermission(principal, course.getTeacherMemberId());
        ensureCourseNotEnded(course);
        if (!assignment.isActive()) {
            assignment.activate();
            studentCourseAssignmentRepository.save(assignment);
            StudentCourseRecord record = loadRecord(assignment);
            if (record.isDeleted()) {
                record.restore();
            }
            studentCourseRecordRepository.save(record);
            clinicDefaultSlotService.createAttendancesForCurrentWeekIfPossible(record, course);
        }
        return StudentCourseAssignmentResponse.from(assignment);
    }

    @Transactional
    public StudentCourseAssignmentResponse deactivateAssignment(MemberPrincipal principal, UUID assignmentId) {
        StudentCourseAssignment assignment = loadAssignment(assignmentId);
        Course course = loadCourse(assignment.getCourseId());
        ensurePermission(principal, course.getTeacherMemberId());
        ensureCourseNotEnded(course);
        if (assignment.isActive()) {
            assignment.deactivate();
            studentCourseAssignmentRepository.save(assignment);
            StudentCourseRecord record = loadRecord(assignment);
            if (!record.isDeleted()) {
                record.delete();
            }
            studentCourseRecordRepository.save(record);
            LocalDateTime now = LocalDateTime.now(KstTime.clock());
            clinicAttendanceRepository.deleteUpcomingAttendances(record.getId(), now.toLocalDate(), now.toLocalTime());
        }
        return StudentCourseAssignmentResponse.from(assignment);
    }

    private Course loadCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (course.isDeleted()) {
            throw new BusinessException(RsCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private StudentCourseAssignment loadAssignment(UUID assignmentId) {
        return studentCourseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_COURSE_ASSIGNMENT_NOT_FOUND));
    }

    private StudentCourseRecord loadRecord(StudentCourseAssignment assignment) {
        return studentCourseRecordRepository
                .findByStudentMemberIdAndCourseId(assignment.getStudentMemberId(), assignment.getCourseId())
                .orElseThrow(RsCode.STUDENT_COURSE_RECORD_NOT_FOUND::toException);
    }

    private void ensureCourseNotEnded(Course course) {
        if (course.getEndDate().isBefore(LocalDate.now(KstTime.clock()))) {
            throw new BusinessException(RsCode.COURSE_ENDED);
        }
    }

    private void ensurePermission(MemberPrincipal principal, UUID teacherId) {
        if (principal.role() == MemberRole.TEACHER) {
            if (!teacherId.equals(principal.id())) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            boolean allowed = assistantAssignmentRepository
                    .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, principal.id())
                    .isPresent();
            if (!allowed) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }

    private void ensureTeacherPermission(MemberPrincipal principal, UUID teacherId) {
        if (principal.role() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        if (!teacherId.equals(principal.id())) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
