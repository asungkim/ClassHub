package com.classhub.domain.progress.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.classhub.domain.progress.personal.dto.request.PersonalProgressComposeRequest;
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
import java.util.Set;
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
class CourseProgressServiceTest {

    @Mock
    private CourseProgressRepository courseProgressRepository;
    @Mock
    private PersonalProgressRepository personalProgressRepository;
    @Mock
    private ProgressPermissionValidator permissionValidator;
    @Mock
    private CourseProgressMapper courseProgressMapper;

    @InjectMocks
    private CourseProgressService courseProgressService;

    private MemberPrincipal teacherPrincipal;
    private MemberPrincipal assistantPrincipal;
    private UUID teacherId;
    private UUID assistantId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        teacherPrincipal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        assistantPrincipal = new MemberPrincipal(assistantId, MemberRole.ASSISTANT);
    }

    @Test
    void createCourseProgress_shouldSaveForTeacher() {
        CourseProgressCreateRequest request = new CourseProgressCreateRequest(
                LocalDate.of(2024, Month.MARCH, 5),
                "Lesson",
                "content"
        );
        Course course = createCourse(courseId, teacherId);
        given(permissionValidator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.WRITE))
                .willReturn(course);

        CourseProgress saved = buildCourseProgress(courseId, teacherId, request.date(), "Lesson");
        given(courseProgressRepository.save(any(CourseProgress.class))).willReturn(saved);

        CourseProgressResponse mockResponse = new CourseProgressResponse(
                saved.getId(),
                courseId,
                request.date(),
                "Lesson",
                "content",
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                saved.getCreatedAt()
        );
        given(courseProgressMapper.toResponse(any(CourseProgress.class)))
                .willReturn(mockResponse);

        CourseProgressResponse response = courseProgressService.createCourseProgress(
                teacherPrincipal,
                courseId,
                request
        );

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.writerId()).isEqualTo(teacherId);
        assertThat(response.title()).isEqualTo("Lesson");
        verify(courseProgressRepository).save(any(CourseProgress.class));
    }

    @Test
    void composeCourseProgress_shouldSaveCourseAndPersonalProgress() {
        Course course = createCourse(courseId, teacherId);
        CourseProgressComposeRequest request = new CourseProgressComposeRequest(
                new CourseProgressCreateRequest(LocalDate.of(2024, Month.MARCH, 3), "Shared", "memo"),
                List.of(
                        new PersonalProgressComposeRequest(UUID.randomUUID(), LocalDate.of(2024, Month.MARCH, 3), "A", "memo"),
                        new PersonalProgressComposeRequest(UUID.randomUUID(), LocalDate.of(2024, Month.MARCH, 3), "B", "memo")
                )
        );
        StudentCourseRecord recordA = createRecord(request.personalProgressList().get(0).studentCourseRecordId(), courseId);
        StudentCourseRecord recordB = createRecord(request.personalProgressList().get(1).studentCourseRecordId(), courseId);
        given(permissionValidator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.WRITE))
                .willReturn(course);
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, recordA.getId(), ProgressAccessMode.WRITE))
                .willReturn(recordA);
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, recordB.getId(), ProgressAccessMode.WRITE))
                .willReturn(recordB);
        CourseProgress saved = buildCourseProgress(courseId, teacherId, request.courseProgress().date(), "Shared");
        given(courseProgressRepository.save(any(CourseProgress.class))).willReturn(saved);
        given(personalProgressRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        CourseProgressResponse mockResponse = new CourseProgressResponse(
                saved.getId(),
                courseId,
                request.courseProgress().date(),
                "Shared",
                "memo",
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                saved.getCreatedAt()
        );
        given(courseProgressMapper.toResponse(any(CourseProgress.class)))
                .willReturn(mockResponse);

        CourseProgressResponse response = courseProgressService.composeCourseProgress(
                teacherPrincipal,
                courseId,
                request
        );

        assertThat(response.id()).isEqualTo(saved.getId());
        verify(courseProgressRepository).save(any(CourseProgress.class));
        verify(personalProgressRepository).saveAll(any());
    }

    @Test
    void composeCourseProgress_shouldRejectMismatchedRecord() {
        Course course = createCourse(courseId, teacherId);
        CourseProgressComposeRequest request = new CourseProgressComposeRequest(
                new CourseProgressCreateRequest(LocalDate.of(2024, Month.MARCH, 3), "Shared", "memo"),
                List.of(new PersonalProgressComposeRequest(UUID.randomUUID(), LocalDate.of(2024, Month.MARCH, 3), "A", "memo"))
        );
        StudentCourseRecord record = createRecord(request.personalProgressList().get(0).studentCourseRecordId(), UUID.randomUUID());
        given(permissionValidator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.WRITE))
                .willReturn(course);
        given(permissionValidator.ensureRecordAccess(teacherPrincipal, record.getId(), ProgressAccessMode.WRITE))
                .willReturn(record);

        assertThatThrownBy(() -> courseProgressService.composeCourseProgress(teacherPrincipal, courseId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);

        verify(courseProgressRepository, never()).save(any());
        verify(personalProgressRepository, never()).saveAll(any());
    }

    @Test
    void getCourseProgresses_shouldBuildCursorWhenFullPage() {
        Course course = createCourse(courseId, teacherId);
        given(permissionValidator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.READ))
                .willReturn(course);
        CourseProgress first = buildCourseProgress(courseId, teacherId, LocalDate.of(2024, Month.MARCH, 5), "C");
        CourseProgress second = buildCourseProgress(courseId, teacherId, LocalDate.of(2024, Month.MARCH, 4), "B");
        given(courseProgressRepository.findRecentByCourseId(eq(courseId), any(), any(), any(Pageable.class)))
                .willReturn(List.of(first, second));

        CourseProgressResponse firstResponse = new CourseProgressResponse(
                first.getId(),
                courseId,
                first.getDate(),
                first.getTitle(),
                first.getContent(),
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                first.getCreatedAt()
        );
        CourseProgressResponse secondResponse = new CourseProgressResponse(
                second.getId(),
                courseId,
                second.getDate(),
                second.getTitle(),
                second.getContent(),
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                second.getCreatedAt()
        );
        given(courseProgressMapper.toResponse(eq(first)))
                .willReturn(firstResponse);
        given(courseProgressMapper.toResponse(eq(second)))
                .willReturn(secondResponse);

        ProgressSliceResponse<CourseProgressResponse> response = courseProgressService.getCourseProgresses(
                teacherPrincipal,
                courseId,
                null,
                null,
                2
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.nextCursor().id()).isEqualTo(second.getId());
    }

    @Test
    void updateCourseProgress_shouldApplyChanges() {
        Course course = createCourse(courseId, teacherId);
        CourseProgress progress = buildCourseProgress(courseId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(courseProgressRepository.findById(progress.getId())).willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureCourseAccess(teacherPrincipal, courseId, ProgressAccessMode.WRITE))
                .willReturn(course);
        given(courseProgressRepository.save(any(CourseProgress.class))).willReturn(progress);

        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 2),
                "New",
                "updated"
        );

        CourseProgressResponse mockResponse = new CourseProgressResponse(
                progress.getId(),
                courseId,
                request.date(),
                "New",
                "updated",
                teacherId,
                "테스트 선생님",
                MemberRole.TEACHER,
                progress.getCreatedAt()
        );
        given(courseProgressMapper.toResponse(any(CourseProgress.class)))
                .willReturn(mockResponse);

        CourseProgressResponse response = courseProgressService.updateCourseProgress(
                teacherPrincipal,
                progress.getId(),
                request
        );

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.content()).isEqualTo("updated");
    }

    @Test
    void updateCourseProgress_shouldThrow_whenAssistantNotWriter() {
        Course course = createCourse(courseId, teacherId);
        CourseProgress progress = buildCourseProgress(courseId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(courseProgressRepository.findById(progress.getId())).willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureCourseAccess(assistantPrincipal, courseId, ProgressAccessMode.WRITE))
                .willReturn(course);

        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                LocalDate.of(2024, Month.MARCH, 2),
                "New",
                "updated"
        );

        assertThatThrownBy(() -> courseProgressService.updateCourseProgress(assistantPrincipal, progress.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void deleteCourseProgress_shouldThrow_whenAssistantNotWriter() {
        Course course = createCourse(courseId, teacherId);
        CourseProgress progress = buildCourseProgress(courseId, teacherId, LocalDate.of(2024, Month.MARCH, 1), "Old");
        given(courseProgressRepository.findById(progress.getId())).willReturn(java.util.Optional.of(progress));
        given(permissionValidator.ensureCourseAccess(assistantPrincipal, courseId, ProgressAccessMode.WRITE))
                .willReturn(course);

        assertThatThrownBy(() -> courseProgressService.deleteCourseProgress(assistantPrincipal, progress.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    private Course createCourse(UUID id, UUID ownerId) {
        Course course = Course.create(
                UUID.randomUUID(),
                ownerId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private StudentCourseRecord createRecord(UUID id, UUID courseId) {
        StudentCourseRecord record = StudentCourseRecord.create(UUID.randomUUID(), courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    private CourseProgress buildCourseProgress(UUID courseId,
                                               UUID writerId,
                                               LocalDate date,
                                               String title) {
        CourseProgress progress = CourseProgress.builder()
                .courseId(courseId)
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
