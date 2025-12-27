package com.classhub.domain.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseArchiveServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseArchiveService courseArchiveService;

    @Test
    void archiveExpiredCourses_shouldSoftDeleteExpiredCourses() {
        LocalDate today = LocalDate.of(2024, 3, 10);
        LocalDate threshold = today.minusDays(7);
        Course courseOne = createCourse(LocalDate.of(2024, 2, 20));
        Course courseTwo = createCourse(LocalDate.of(2024, 3, 1));
        given(courseRepository.findByEndDateLessThanEqualAndDeletedAtIsNull(threshold))
                .willReturn(List.of(courseOne, courseTwo));

        int archived = courseArchiveService.archiveExpiredCourses(today);

        assertThat(archived).isEqualTo(2);
        assertThat(courseOne.isDeleted()).isTrue();
        assertThat(courseTwo.isDeleted()).isTrue();
        verify(courseRepository).saveAll(List.of(courseOne, courseTwo));
    }

    @Test
    void archiveExpiredCourses_shouldSkipWhenNoCourses() {
        LocalDate today = LocalDate.of(2024, 3, 10);
        LocalDate threshold = today.minusDays(7);
        given(courseRepository.findByEndDateLessThanEqualAndDeletedAtIsNull(threshold))
                .willReturn(List.of());

        int archived = courseArchiveService.archiveExpiredCourses(today);

        assertThat(archived).isEqualTo(0);
        verify(courseRepository, never()).saveAll(any());
    }

    @Test
    void archiveExpiredCourses_shouldThrow_whenTodayMissing() {
        assertThatThrownBy(() -> courseArchiveService.archiveExpiredCourses(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
        verify(courseRepository, never()).findByEndDateLessThanEqualAndDeletedAtIsNull(any());
    }

    private Course createCourse(LocalDate endDate) {
        return Course.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Course",
                "Desc",
                endDate.minusMonths(1),
                endDate,
                java.util.Set.of()
        );
    }
}
