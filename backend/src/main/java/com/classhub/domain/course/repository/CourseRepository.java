package com.classhub.domain.course.repository;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.model.Course;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    default Page<Course> searchCourses(
            UUID teacherId,
            UUID branchId,
            CourseStatusFilter status,
            String keyword,
            Pageable pageable
    ) {
        CourseStatusFilter effective = status == null ? CourseStatusFilter.ALL : status;
        boolean activeOnly = effective == CourseStatusFilter.ACTIVE;
        boolean inactiveOnly = effective == CourseStatusFilter.INACTIVE;
        return searchCoursesInternal(teacherId, branchId, activeOnly, inactiveOnly, keyword, pageable);
    }

    @Query("""
            SELECT c
            FROM Course c
            WHERE c.teacherMemberId = :teacherId
              AND (:branchId IS NULL OR c.branchId = :branchId)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:activeOnly = false OR c.deletedAt IS NULL)
              AND (:inactiveOnly = false OR c.deletedAt IS NOT NULL)
            """)
    Page<Course> searchCoursesInternal(
            @Param("teacherId") UUID teacherId,
            @Param("branchId") UUID branchId,
            @Param("activeOnly") boolean activeOnly,
            @Param("inactiveOnly") boolean inactiveOnly,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM Course c
            WHERE c.teacherMemberId = :teacherId
              AND c.deletedAt IS NULL
              AND c.startDate <= :endDate
              AND c.endDate >= :startDate
            """)
    List<Course> findCoursesWithinPeriod(
            @Param("teacherId") UUID teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT c
            FROM Course c
            LEFT JOIN FETCH c.schedules s
            WHERE c.teacherMemberId = :teacherId
              AND c.deletedAt IS NULL
              AND c.startDate <= :endDate
              AND c.endDate >= :startDate
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    List<Course> findOverlappingCourses(
            @Param("teacherId") UUID teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") UUID excludeId
    );

    default Page<Course> searchCoursesForAdmin(UUID teacherId,
                                               UUID branchId,
                                               UUID companyId,
                                               CourseStatusFilter status,
                                               String keyword,
                                               Pageable pageable) {
        CourseStatusFilter effective = status == null ? CourseStatusFilter.ALL : status;
        boolean activeOnly = effective == CourseStatusFilter.ACTIVE;
        boolean inactiveOnly = effective == CourseStatusFilter.INACTIVE;
        return searchCoursesForAdminInternal(
                teacherId,
                branchId,
                companyId,
                activeOnly,
                inactiveOnly,
                keyword,
                pageable
        );
    }

    @Query("""
            SELECT c
            FROM Course c
            JOIN Branch b ON b.id = c.branchId
            WHERE (:teacherId IS NULL OR c.teacherMemberId = :teacherId)
              AND (:branchId IS NULL OR c.branchId = :branchId)
              AND (:companyId IS NULL OR b.companyId = :companyId)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:activeOnly = false OR c.deletedAt IS NULL)
              AND (:inactiveOnly = false OR c.deletedAt IS NOT NULL)
            """)
    Page<Course> searchCoursesForAdminInternal(
            @Param("teacherId") UUID teacherId,
            @Param("branchId") UUID branchId,
            @Param("companyId") UUID companyId,
            @Param("activeOnly") boolean activeOnly,
            @Param("inactiveOnly") boolean inactiveOnly,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    default Page<Course> searchCoursesForAssistant(Collection<UUID> teacherIds,
                                                   CourseStatusFilter status,
                                                   String keyword,
                                                   Pageable pageable) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Page.empty(pageable);
        }
        CourseStatusFilter effective = status == null ? CourseStatusFilter.ALL : status;
        boolean activeOnly = effective == CourseStatusFilter.ACTIVE;
        boolean inactiveOnly = effective == CourseStatusFilter.INACTIVE;
        return searchCoursesForAssistantInternal(
                teacherIds,
                activeOnly,
                inactiveOnly,
                keyword,
                pageable
        );
    }

    @Query("""
            SELECT c
            FROM Course c
            WHERE c.teacherMemberId IN :teacherIds
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:activeOnly = false OR c.deletedAt IS NULL)
              AND (:inactiveOnly = false OR c.deletedAt IS NOT NULL)
            """)
    Page<Course> searchCoursesForAssistantInternal(
            @Param("teacherIds") Collection<UUID> teacherIds,
            @Param("activeOnly") boolean activeOnly,
            @Param("inactiveOnly") boolean inactiveOnly,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    default Page<Course> searchPublicCourses(UUID companyId,
                                             UUID branchId,
                                             UUID teacherId,
                                             String keyword,
                                             boolean onlyVerified,
                                             Pageable pageable) {
        return searchPublicCoursesInternal(
                companyId,
                branchId,
                teacherId,
                keyword,
                onlyVerified,
                pageable
        );
    }

    @Query("""
            SELECT c
            FROM Course c
            JOIN Branch b ON b.id = c.branchId
            JOIN Company comp ON comp.id = b.companyId
            WHERE c.deletedAt IS NULL
              AND b.deletedAt IS NULL
              AND comp.deletedAt IS NULL
              AND (:companyId IS NULL OR comp.id = :companyId)
              AND (:branchId IS NULL OR b.id = :branchId)
              AND (:teacherId IS NULL OR c.teacherMemberId = :teacherId)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (
                    :onlyVerified = false OR
                    (b.verifiedStatus = com.classhub.domain.company.company.model.VerifiedStatus.VERIFIED
                     AND comp.verifiedStatus = com.classhub.domain.company.company.model.VerifiedStatus.VERIFIED)
              )
            """)
    Page<Course> searchPublicCoursesInternal(
            @Param("companyId") UUID companyId,
            @Param("branchId") UUID branchId,
            @Param("teacherId") UUID teacherId,
            @Param("keyword") String keyword,
            @Param("onlyVerified") boolean onlyVerified,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM Course c
            WHERE c.teacherMemberId = :teacherId
              AND c.deletedAt IS NULL
              AND c.endDate >= :today
              AND (:branchId IS NULL OR c.branchId = :branchId)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Course> searchAssignableCoursesForTeacher(@Param("teacherId") UUID teacherId,
                                                   @Param("branchId") UUID branchId,
                                                   @Param("keyword") String keyword,
                                                   @Param("today") LocalDate today,
                                                   Pageable pageable);

    default Page<Course> searchAssignableCoursesForTeachers(Collection<UUID> teacherIds,
                                                            UUID branchId,
                                                            String keyword,
                                                            LocalDate today,
                                                            Pageable pageable) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return searchAssignableCoursesForTeachersInternal(teacherIds, branchId, keyword, today, pageable);
    }

    @Query("""
            SELECT c
            FROM Course c
            WHERE c.teacherMemberId IN :teacherIds
              AND c.deletedAt IS NULL
              AND c.endDate >= :today
              AND (:branchId IS NULL OR c.branchId = :branchId)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Course> searchAssignableCoursesForTeachersInternal(@Param("teacherIds") Collection<UUID> teacherIds,
                                                            @Param("branchId") UUID branchId,
                                                            @Param("keyword") String keyword,
                                                            @Param("today") LocalDate today,
                                                            Pageable pageable);

    java.util.Optional<Course> findByTeacherMemberIdAndBranchIdAndName(UUID teacherMemberId,
                                                                       UUID branchId,
                                                                       String name);
}
