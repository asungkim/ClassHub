package com.classhub.domain.calendar.mapper;

import com.classhub.domain.calendar.dto.StudentCalendarResponse.ClinicEvent;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.ClinicRecordSummary;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.CourseProgressEvent;
import com.classhub.domain.calendar.dto.StudentCalendarResponse.PersonalProgressEvent;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceEventProjection;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.course.model.CourseProgress;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StudentCalendarMapper {

    public List<CourseProgressEvent> toCourseProgressEvents(List<CourseProgress> progresses,
                                                            Map<UUID, Course> courseMap,
                                                            Map<UUID, MemberRole> roleMap,
                                                            Map<UUID, String> nameMap) {
        return progresses.stream()
                .map(progress -> new CourseProgressEvent(
                        progress.getId(),
                        progress.getCourseId(),
                        resolveCourseName(courseMap, progress.getCourseId()),
                        progress.getDate(),
                        progress.getTitle(),
                        progress.getContent(),
                        progress.getWriterId(),
                        resolveWriterName(nameMap, progress.getWriterId()),
                        resolveWriterRole(roleMap, progress.getWriterId()),
                        progress.getCreatedAt()
                ))
                .toList();
    }

    public List<PersonalProgressEvent> toPersonalProgressEvents(List<PersonalProgress> progresses,
                                                                Map<UUID, UUID> recordCourseMap,
                                                                Map<UUID, Course> courseMap,
                                                                Map<UUID, MemberRole> roleMap,
                                                                Map<UUID, String> nameMap) {
        return progresses.stream()
                .map(progress -> {
                    UUID courseId = resolveCourseId(recordCourseMap, progress.getStudentCourseRecordId());
                    return new PersonalProgressEvent(
                            progress.getId(),
                            progress.getStudentCourseRecordId(),
                            courseId,
                            resolveCourseName(courseMap, courseId),
                            progress.getDate(),
                            progress.getTitle(),
                            progress.getContent(),
                            resolveWriterName(nameMap, progress.getWriterId()),
                            resolveWriterRole(roleMap, progress.getWriterId()),
                            progress.getCreatedAt()
                    );
                })
                .toList();
    }

    public List<ClinicEvent> toClinicEvents(List<ClinicAttendanceEventProjection> events,
                                            Map<UUID, MemberRole> roleMap,
                                            Map<UUID, String> nameMap) {
        return events.stream()
                .map(event -> new ClinicEvent(
                        event.getClinicSessionId(),
                        event.getClinicAttendanceId(),
                        event.getCourseId(),
                        event.getSlotId(),
                        event.getDate(),
                        event.getStartTime(),
                        event.getEndTime(),
                        event.isCanceled(),
                        buildRecordSummary(event, roleMap, nameMap)
                ))
                .toList();
    }

    private ClinicRecordSummary buildRecordSummary(ClinicAttendanceEventProjection event,
                                                   Map<UUID, MemberRole> roleMap,
                                                   Map<UUID, String> nameMap) {
        if (event.getRecordId() == null) {
            return null;
        }
        MemberRole writerRole = resolveWriterRole(roleMap, event.getRecordWriterId());
        String writerName = resolveWriterName(nameMap, event.getRecordWriterId());
        return new ClinicRecordSummary(
                event.getRecordId(),
                event.getRecordTitle(),
                event.getRecordContent(),
                event.getRecordHomeworkProgress(),
                writerName,
                writerRole,
                event.getRecordCreatedAt()
        );
    }

    private String resolveCourseName(Map<UUID, Course> courseMap, UUID courseId) {
        Course course = courseMap.get(courseId);
        if (course == null) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return course.getName();
    }

    private UUID resolveCourseId(Map<UUID, UUID> recordCourseMap, UUID recordId) {
        UUID courseId = recordCourseMap.get(recordId);
        if (courseId == null) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return courseId;
    }

    private MemberRole resolveWriterRole(Map<UUID, MemberRole> roleMap, UUID writerId) {
        MemberRole role = roleMap.get(writerId);
        if (role == null) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return role;
    }

    private String resolveWriterName(Map<UUID, String> nameMap, UUID writerId) {
        String name = nameMap.get(writerId);
        if (name == null) {
            throw new BusinessException(RsCode.INTERNAL_SERVER);
        }
        return name;
    }
}
