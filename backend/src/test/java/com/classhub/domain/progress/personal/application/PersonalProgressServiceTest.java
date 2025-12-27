package com.classhub.domain.progress.personal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.progress.dto.ProgressSliceResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PersonalProgressServiceTest {

    @Mock
    private PersonalProgressRepository personalProgressRepository;
    @Mock
    private ProgressPermissionValidator permissionValidator;
    @Mock
    private PersonalProgressMapper personalProgressMapper;

    @InjectMocks
    private PersonalProgressService personalProgressService;

    private MemberPrincipal teacherPrincipal;
    private MemberPrincipal assistantPrincipal;
    private UUID teacherId;
    private UUID assistantId;
    private UUID recordId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        recordId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        teacherPrincipal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        assistantPrincipal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
    }

    @Test
    void createPersonalProgress_shouldSaveForTeacher() {
        PersonalProgressCreateRequest request = new PersonalProgressCreateRequest(
                LocalDate.of(2024, Month.MARCH, 5),
                "Note",
                "memo"
        );
        StudentCourseRecord record = createRecord(recordId, courseId);
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, recordId, ProgressAccessMode.WRITE))
                .willReturn(record);
        PersonalProgress saved = buildPersonalProgress(recordId, teacherId, request.date(), "Note");
        given(personalProgressRepository.save(any(PersonalProgress.class))).willReturn(saved);

        PersonalProgressResponse mockResponse = new PersonalProgressResponse(
                saved.getId(),
                recordId,
                courseId,
                request.date(),
                "Note",
                "memo",
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                saved.getCreatedAt()
        );
        given(personalProgressMapper.toResponse(any(PersonalProgress.class), eq(courseId)))
                .willReturn(mockResponse);

        PersonalProgressResponse response = personalProgressService.createPersonalProgress(
                teacherPrincipal,
                recordId,
                request
        );

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.courseId()).isEqualTo(courseId);
        verify(personalProgressRepository).save(any(PersonalProgress.class));
    }

    @Test
    void getPersonalProgresses_shouldBuildCursorWhenFullPage() {
        StudentCourseRecord record = createRecord(recordId, courseId);
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, recordId, ProgressAccessMode.READ))
                .willReturn(record);
        PersonalProgress first = buildPersonalProgress(recordId, teacherId, LocalDate.of(2024, Month.MARCH, 6), "B");
        PersonalProgress second = buildPersonalProgress(recordId, teacherId, LocalDate.of(2024, Month.MARCH, 5), "A");
        given(personalProgressRepository.findRecentByRecordId(eq(recordId), any(), any(), any(Pageable.class)))
                .willReturn(List.of(first, second));

        PersonalProgressResponse firstResponse = new PersonalProgressResponse(
                first.getId(),
                recordId,
                courseId,
                first.getDate(),
                first.getTitle(),
                first.getContent(),
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                first.getCreatedAt()
        );
        PersonalProgressResponse secondResponse = new PersonalProgressResponse(
                second.getId(),
                recordId,
                courseId,
                second.getDate(),
                second.getTitle(),
                second.getContent(),
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                second.getCreatedAt()
        );
        given(personalProgressMapper.toResponse(eq(first), eq(courseId)))
                .willReturn(firstResponse);
        given(personalProgressMapper.toResponse(eq(second), eq(courseId)))
                .willReturn(secondResponse);

        ProgressSliceResponse<PersonalProgressResponse> response = personalProgressService.getPersonalProgresses(
                teacherPrincipal,
                recordId,
                null,
                null,
                2
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.nextCursor().id()).isEqualTo(second.getId());
    }

    @Test
    void updatePersonalProgress_shouldApplyChanges() {
        StudentCourseRecord record = createRecord(recordId, courseId);
        PersonalProgress progress = buildPersonalProgress(recordId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(personalProgressRepository.findById(progress.getId()))
                .willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, recordId, ProgressAccessMode.WRITE))
                .willReturn(record);
        given(personalProgressRepository.save(any(PersonalProgress.class))).willReturn(progress);

        PersonalProgressUpdateRequest request = new PersonalProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 2),
                "New",
                "updated"
        );

        PersonalProgressResponse mockResponse = new PersonalProgressResponse(
                progress.getId(),
                recordId,
                courseId,
                request.date(),
                "New",
                "updated",
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                progress.getCreatedAt()
        );
        given(personalProgressMapper.toResponse(any(PersonalProgress.class), eq(courseId)))
                .willReturn(mockResponse);

        PersonalProgressResponse response = personalProgressService.updatePersonalProgress(
                teacherPrincipal,
                progress.getId(),
                request
        );

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.content()).isEqualTo("updated");
    }

    @Test
    void deletePersonalProgress_shouldRemoveEntity() {
        StudentCourseRecord record = createRecord(recordId, courseId);
        PersonalProgress progress = buildPersonalProgress(recordId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(personalProgressRepository.findById(progress.getId()))
                .willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, recordId, ProgressAccessMode.WRITE))
                .willReturn(record);

        personalProgressService.deletePersonalProgress(teacherPrincipal, progress.getId());

        verify(personalProgressRepository).delete(progress);
    }

    @Test
    void updatePersonalProgress_shouldThrow_whenAssistantNotWriter() {
        StudentCourseRecord record = createRecord(recordId, courseId);
        PersonalProgress progress = buildPersonalProgress(recordId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(personalProgressRepository.findById(progress.getId()))
                .willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureRecordAccess(assistantPrincipal, recordId, ProgressAccessMode.WRITE))
                .willReturn(record);

        PersonalProgressUpdateRequest request = new PersonalProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 2),
                "New",
                "updated"
        );

        assertThatThrownBy(() -> personalProgressService.updatePersonalProgress(assistantPrincipal, progress.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void deletePersonalProgress_shouldThrow_whenAssistantNotWriter() {
        StudentCourseRecord record = createRecord(recordId, courseId);
        PersonalProgress progress = buildPersonalProgress(recordId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(personalProgressRepository.findById(progress.getId()))
                .willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureRecordAccess(assistantPrincipal, recordId, ProgressAccessMode.WRITE))
                .willReturn(record);

        assertThatThrownBy(() -> personalProgressService.deletePersonalProgress(assistantPrincipal, progress.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    private StudentCourseRecord createRecord(UUID recordId, UUID courseId) {
        StudentCourseRecord record = StudentCourseRecord.create(UUID.randomUUID(), courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", recordId);
        return record;
    }

    private PersonalProgress buildPersonalProgress(UUID recordId,
                                                   UUID writerId,
                                                   LocalDate date,
                                                   String title) {
        PersonalProgress progress = PersonalProgress.builder()
                .studentCourseRecordId(recordId)
                .writerId(writerId)
                .date(date)
                .title(title)
                .content("memo")
                .build();
        ReflectionTestUtils.setField(progress, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(progress, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(progress, "updatedAt", LocalDateTime.now());
        return progress;
    }
}
