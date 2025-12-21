package com.classhub.domain.progress.course.application;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.course.dto.request.CourseProgressComposeRequest;
import com.classhub.domain.progress.course.dto.request.CourseProgressCreateRequest;
import com.classhub.domain.progress.course.dto.request.CourseProgressUpdateRequest;
import com.classhub.domain.progress.course.dto.response.CourseProgressResponse;
import com.classhub.domain.progress.course.mapper.CourseProgressMapper;
import com.classhub.domain.progress.course.model.CourseProgress;
import com.classhub.domain.progress.course.repository.CourseProgressRepository;
import com.classhub.domain.progress.dto.ProgressSliceResponse;
import com.classhub.domain.progress.dto.ProgressSliceResponse.ProgressCursor;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressComposeRequest;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import com.classhub.domain.progress.personal.repository.PersonalProgressRepository;
import com.classhub.domain.progress.support.ProgressPermissionValidator;
import com.classhub.domain.progress.support.ProgressPermissionValidator.ProgressAccessMode;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseProgressService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final CourseProgressRepository courseProgressRepository;
    private final PersonalProgressRepository personalProgressRepository;
    private final ProgressPermissionValidator permissionValidator;
    private final CourseProgressMapper courseProgressMapper;

    public CourseProgressResponse createCourseProgress(MemberPrincipal principal,
                                                       UUID courseId,
                                                       CourseProgressCreateRequest request) {
        Course course = permissionValidator.ensureCourseAccess(principal, courseId, ProgressAccessMode.WRITE);
        CourseProgress progress = CourseProgress.builder()
                .courseId(course.getId())
                .writerId(principal.id())
                .date(request.date())
                .title(request.title())
                .content(request.content())
                .build();
        CourseProgress saved = courseProgressRepository.save(progress);
        return courseProgressMapper.toResponse(saved, MemberRole.TEACHER);
    }

    public CourseProgressResponse composeCourseProgress(MemberPrincipal principal,
                                                        UUID courseId,
                                                        CourseProgressComposeRequest request) {
        Course course = permissionValidator.ensureCourseAccess(principal, courseId, ProgressAccessMode.WRITE);
        List<PersonalProgress> personalProgresses = request.personalProgressList().stream()
                .map(personalRequest -> toPersonalProgress(principal, course.getId(), personalRequest))
                .toList();
        CourseProgress courseProgress = CourseProgress.builder()
                .courseId(course.getId())
                .writerId(principal.id())
                .date(request.courseProgress().date())
                .title(request.courseProgress().title())
                .content(request.courseProgress().content())
                .build();
        CourseProgress saved = courseProgressRepository.save(courseProgress);
        if (!personalProgresses.isEmpty()) {
            personalProgressRepository.saveAll(personalProgresses);
        }
        return courseProgressMapper.toResponse(saved, MemberRole.TEACHER);
    }

    @Transactional(readOnly = true)
    public ProgressSliceResponse<CourseProgressResponse> getCourseProgresses(MemberPrincipal principal,
                                                                             UUID courseId,
                                                                             LocalDateTime cursorCreatedAt,
                                                                             UUID cursorId,
                                                                             Integer limit) {
        permissionValidator.ensureCourseAccess(principal, courseId, ProgressAccessMode.READ);
        validateCursor(cursorCreatedAt, cursorId);
        int pageSize = resolveLimit(limit);
        List<CourseProgress> progressList = courseProgressRepository.findRecentByCourseId(
                courseId,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, pageSize)
        );
        List<CourseProgressResponse> items = progressList.stream()
                .map(progress -> courseProgressMapper.toResponse(progress, MemberRole.TEACHER))
                .toList();
        ProgressCursor nextCursor = resolveNextCursor(progressList, pageSize);
        return new ProgressSliceResponse<>(items, nextCursor);
    }

    public CourseProgressResponse updateCourseProgress(MemberPrincipal principal,
                                                       UUID progressId,
                                                       CourseProgressUpdateRequest request) {
        CourseProgress progress = loadProgress(progressId);
        permissionValidator.ensureCourseAccess(principal, progress.getCourseId(), ProgressAccessMode.WRITE);
        progress.update(request.date(), request.title(), request.content());
        CourseProgress saved = courseProgressRepository.save(progress);
        return courseProgressMapper.toResponse(saved, MemberRole.TEACHER);
    }

    public void deleteCourseProgress(MemberPrincipal principal, UUID progressId) {
        CourseProgress progress = loadProgress(progressId);
        permissionValidator.ensureCourseAccess(principal, progress.getCourseId(), ProgressAccessMode.WRITE);
        courseProgressRepository.delete(progress);
    }

    private CourseProgress loadProgress(UUID progressId) {
        return courseProgressRepository.findById(progressId)
                .orElseThrow(() -> new BusinessException(RsCode.SHARED_LESSON_NOT_FOUND));
    }

    private PersonalProgress toPersonalProgress(MemberPrincipal principal,
                                                UUID courseId,
                                                PersonalProgressComposeRequest request) {
        StudentCourseRecord record = permissionValidator.ensureRecordAccess(
                principal,
                request.studentCourseRecordId(),
                ProgressAccessMode.WRITE
        );
        if (!record.getCourseId().equals(courseId)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return PersonalProgress.builder()
                .studentCourseRecordId(record.getId())
                .writerId(principal.id())
                .date(request.date())
                .title(request.title())
                .content(request.content())
                .build();
    }

    private ProgressCursor resolveNextCursor(List<CourseProgress> progressList, int limit) {
        if (progressList.isEmpty() || progressList.size() < limit) {
            return null;
        }
        CourseProgress last = progressList.get(progressList.size() - 1);
        return new ProgressCursor(last.getId(), last.getCreatedAt());
    }

    private void validateCursor(LocalDateTime cursorCreatedAt, UUID cursorId) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return limit;
    }
}
