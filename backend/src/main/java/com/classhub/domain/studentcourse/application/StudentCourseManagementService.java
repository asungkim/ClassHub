package com.classhub.domain.studentcourse.application;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.slot.application.ClinicDefaultSlotService;
import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.domain.studentcourse.dto.request.StudentCourseRecordUpdateRequest;
import com.classhub.domain.studentcourse.dto.response.StudentCourseDetailResponse;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.util.KstTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCourseManagementService {

    private final StudentCourseRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final TeacherAssistantAssignmentRepository assignmentRepository;
    private final CourseViewAssembler courseViewAssembler;
    private final ClinicDefaultSlotService clinicDefaultSlotService;

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

    private void ensureTeacher(Course course, UUID teacherId) {
        if (!course.getTeacherMemberId().equals(teacherId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
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
}
