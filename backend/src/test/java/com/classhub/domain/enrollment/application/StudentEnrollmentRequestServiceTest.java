package com.classhub.domain.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.course.application.CourseViewAssembler;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.enrollment.dto.request.StudentEnrollmentRequestCreateRequest;
import com.classhub.domain.enrollment.dto.response.StudentEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.repository.StudentEnrollmentRequestRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseEnrollmentRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentRequestServiceTest {

    @Mock
    private StudentEnrollmentRequestRepository requestRepository;

    @Mock
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseViewAssembler courseViewAssembler;

    @InjectMocks
    private StudentEnrollmentRequestService requestService;

    private UUID studentId;
    private UUID courseId;
    private Course course;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        course = Course.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        courseResponse = new CourseResponse(
                courseId,
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
    }

    @Test
    void createRequest_shouldPersist_whenValid() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(requestRepository.existsByStudentMemberIdAndCourseIdAndStatusIn(eq(studentId), eq(courseId), anyCollection()))
                .thenReturn(false);
        when(enrollmentRepository.existsByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
                .thenReturn(false);
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);
        ArgumentCaptor<StudentEnrollmentRequest> requestCaptor = ArgumentCaptor.forClass(StudentEnrollmentRequest.class);
        UUID savedId = UUID.randomUUID();
        when(requestRepository.save(any(StudentEnrollmentRequest.class))).thenAnswer(invocation -> {
            StudentEnrollmentRequest entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", savedId);
            return entity;
        });

        StudentEnrollmentRequestResponse response = requestService.createRequest(
                studentId,
                new StudentEnrollmentRequestCreateRequest(courseId, "참여하고 싶어요")
        );

        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStudentMemberId()).isEqualTo(studentId);
        assertThat(response.requestId()).isEqualTo(savedId);
        assertThat(response.course().courseId()).isEqualTo(courseId);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void createRequest_shouldThrow_whenPendingRequestExists() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(requestRepository.existsByStudentMemberIdAndCourseIdAndStatusIn(eq(studentId), eq(courseId), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> requestService.createRequest(
                studentId,
                new StudentEnrollmentRequestCreateRequest(courseId, null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.STUDENT_ENROLLMENT_REQUEST_CONFLICT);

        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRequest_shouldThrow_whenAlreadyEnrolled() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(requestRepository.existsByStudentMemberIdAndCourseIdAndStatusIn(eq(studentId), eq(courseId), anyCollection()))
                .thenReturn(false);
        when(enrollmentRepository.existsByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
                .thenReturn(true);

        assertThatThrownBy(() -> requestService.createRequest(
                studentId,
                new StudentEnrollmentRequestCreateRequest(courseId, null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.STUDENT_ENROLLMENT_ALREADY_EXISTS);
    }

    @Test
    void getMyRequests_shouldReturnResponsesFilteredByStatus() {
        StudentEnrollmentRequest request = StudentEnrollmentRequest.builder()
                .studentMemberId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.PENDING)
                .message("안녕하세요")
                .build();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        Page<StudentEnrollmentRequest> page = new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1);
        when(requestRepository.findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(
                eq(studentId),
                anySet(),
                any(PageRequest.class)
        )).thenReturn(page);
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course));
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        PageResponse<StudentEnrollmentRequestResponse> response = requestService.getMyRequests(
                studentId,
                Set.of(EnrollmentStatus.PENDING, EnrollmentStatus.REJECTED),
                0,
                10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().course().name()).isEqualTo("중3 수학");
        verify(requestRepository).findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(
                eq(studentId),
                anySet(),
                eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void cancelRequest_shouldUpdateStatus_whenOwnerAndPending() {
        StudentEnrollmentRequest request = StudentEnrollmentRequest.builder()
                .studentMemberId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.PENDING)
                .message("취소할게요")
                .build();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
        when(courseViewAssembler.buildContext(anyCollection())).thenReturn(context);
        when(courseViewAssembler.toCourseResponse(course, context)).thenReturn(courseResponse);

        StudentEnrollmentRequestResponse response = requestService.cancelRequest(studentId, request.getId());

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELED);
        verify(requestRepository).findById(request.getId());
    }

    @Test
    void cancelRequest_shouldThrow_whenStatusNotPending() {
        StudentEnrollmentRequest request = StudentEnrollmentRequest.builder()
                .studentMemberId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.APPROVED)
                .message("이미 승인")
                .build();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> requestService.cancelRequest(studentId, request.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.INVALID_ENROLLMENT_REQUEST_STATE);
    }
}
