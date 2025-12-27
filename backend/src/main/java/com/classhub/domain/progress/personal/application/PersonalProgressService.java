package com.classhub.domain.progress.personal.application;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.dto.ProgressSliceResponse;
import com.classhub.domain.progress.dto.ProgressSliceResponse.ProgressCursor;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressCreateRequest;
import com.classhub.domain.progress.personal.dto.request.PersonalProgressUpdateRequest;
import com.classhub.domain.progress.personal.dto.response.PersonalProgressResponse;
import com.classhub.domain.progress.personal.mapper.PersonalProgressMapper;
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
public class PersonalProgressService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final PersonalProgressRepository personalProgressRepository;
    private final ProgressPermissionValidator permissionValidator;
    private final PersonalProgressMapper personalProgressMapper;

    public PersonalProgressResponse createPersonalProgress(MemberPrincipal principal,
                                                           UUID recordId,
                                                           PersonalProgressCreateRequest request) {
        StudentCourseRecord record = permissionValidator.ensureRecordAccess(
                principal,
                recordId,
                ProgressAccessMode.WRITE
        );
        PersonalProgress progress = PersonalProgress.builder()
                .studentCourseRecordId(record.getId())
                .writerId(principal.id())
                .date(request.date())
                .title(request.title())
                .content(request.content())
                .build();
        PersonalProgress saved = personalProgressRepository.save(progress);
        return personalProgressMapper.toResponse(saved, record.getCourseId());
    }

    @Transactional(readOnly = true)
    public ProgressSliceResponse<PersonalProgressResponse> getPersonalProgresses(MemberPrincipal principal,
                                                                                 UUID recordId,
                                                                                 LocalDateTime cursorCreatedAt,
                                                                                 UUID cursorId,
                                                                                 Integer limit) {
        StudentCourseRecord record = permissionValidator.ensureRecordAccess(
                principal,
                recordId,
                ProgressAccessMode.READ
        );
        validateCursor(cursorCreatedAt, cursorId);
        int pageSize = resolveLimit(limit);
        List<PersonalProgress> progressList = personalProgressRepository.findRecentByRecordId(
                recordId,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, pageSize)
        );
        List<PersonalProgressResponse> items = progressList.stream()
                .map(progress -> personalProgressMapper.toResponse(progress, record.getCourseId()))
                .toList();
        ProgressCursor nextCursor = resolveNextCursor(progressList, pageSize);
        return new ProgressSliceResponse<>(items, nextCursor);
    }

    public PersonalProgressResponse updatePersonalProgress(MemberPrincipal principal,
                                                           UUID progressId,
                                                           PersonalProgressUpdateRequest request) {
        PersonalProgress progress = loadProgress(progressId);
        StudentCourseRecord record = permissionValidator.ensureRecordAccess(
                principal,
                progress.getStudentCourseRecordId(),
                ProgressAccessMode.WRITE
        );
        ensureWriterAccess(principal, progress.getWriterId());
        progress.update(request.date(), request.title(), request.content());
        PersonalProgress saved = personalProgressRepository.save(progress);
        return personalProgressMapper.toResponse(saved, record.getCourseId());
    }

    public void deletePersonalProgress(MemberPrincipal principal, UUID progressId) {
        PersonalProgress progress = loadProgress(progressId);
        permissionValidator.ensureRecordAccess(
                principal,
                progress.getStudentCourseRecordId(),
                ProgressAccessMode.WRITE
        );
        ensureWriterAccess(principal, progress.getWriterId());
        personalProgressRepository.delete(progress);
    }

    private PersonalProgress loadProgress(UUID progressId) {
        return personalProgressRepository.findById(progressId)
                .orElseThrow(() -> new BusinessException(RsCode.PERSONAL_LESSON_NOT_FOUND));
    }

    private ProgressCursor resolveNextCursor(List<PersonalProgress> progressList, int limit) {
        if (progressList.isEmpty() || progressList.size() < limit) {
            return null;
        }
        PersonalProgress last = progressList.get(progressList.size() - 1);
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

    private void ensureWriterAccess(MemberPrincipal principal, UUID writerId) {
        if (principal.role() == MemberRole.ASSISTANT && !principal.id().equals(writerId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }
}
