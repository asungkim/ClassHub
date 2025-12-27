package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentCourseRecordRepository extends JpaRepository<StudentCourseRecord, UUID> {

    interface DefaultClinicSlotCount {
        UUID getSlotId();
        long getCount();
    }

    long countByDefaultClinicSlotIdAndDeletedAtIsNull(UUID defaultClinicSlotId);

    List<StudentCourseRecord> findByDefaultClinicSlotIdAndDeletedAtIsNull(UUID defaultClinicSlotId);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            WHERE scr.defaultClinicSlotId = :slotId
              AND scr.deletedAt IS NULL
              AND c.deletedAt IS NULL
            """)
    List<StudentCourseRecord> findActiveByDefaultClinicSlotId(@Param("slotId") UUID slotId);

    @Query("""
            SELECT scr.defaultClinicSlotId AS slotId,
                   COUNT(scr) AS count
            FROM StudentCourseRecord scr
            WHERE scr.defaultClinicSlotId IN :slotIds
              AND scr.deletedAt IS NULL
            GROUP BY scr.defaultClinicSlotId
            """)
    List<DefaultClinicSlotCount> countDefaultClinicSlots(@Param("slotIds") List<UUID> slotIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StudentCourseRecord scr
            SET scr.defaultClinicSlotId = null
            WHERE scr.defaultClinicSlotId = :slotId
            """)
    int clearDefaultClinicSlotId(@Param("slotId") UUID slotId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StudentCourseRecord scr
            SET scr.assistantMemberId = null
            WHERE scr.assistantMemberId = :assistantId
              AND scr.courseId IN (
                SELECT c.id
                FROM Course c
                WHERE c.teacherMemberId = :teacherId
              )
            """)
    int clearAssistantMemberId(@Param("teacherId") UUID teacherId,
                               @Param("assistantId") UUID assistantId);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            JOIN Member m ON m.id = scr.studentMemberId
            WHERE c.teacherMemberId = :teacherId
              AND (:courseId IS NULL OR scr.courseId = :courseId)
              AND (:activeOnly = false OR scr.deletedAt IS NULL)
              AND (:inactiveOnly = false OR scr.deletedAt IS NOT NULL)
              AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY m.name ASC
            """)
    Page<StudentCourseRecord> searchRecordsForTeacher(@Param("teacherId") UUID teacherId,
                                                      @Param("courseId") UUID courseId,
                                                      @Param("activeOnly") boolean activeOnly,
                                                      @Param("inactiveOnly") boolean inactiveOnly,
                                                      @Param("keyword") String keyword,
                                                      Pageable pageable);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            JOIN Member m ON m.id = scr.studentMemberId
            WHERE c.teacherMemberId IN :teacherIds
              AND (:courseId IS NULL OR scr.courseId = :courseId)
              AND (:activeOnly = false OR scr.deletedAt IS NULL)
              AND (:inactiveOnly = false OR scr.deletedAt IS NOT NULL)
              AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY m.name ASC
            """)
    Page<StudentCourseRecord> searchRecordsForTeachers(@Param("teacherIds") List<UUID> teacherIds,
                                                       @Param("courseId") UUID courseId,
                                                       @Param("activeOnly") boolean activeOnly,
                                                       @Param("inactiveOnly") boolean inactiveOnly,
                                                       @Param("keyword") String keyword,
                                                       Pageable pageable);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            WHERE scr.studentMemberId = :studentId
              AND c.teacherMemberId = :teacherId
              AND scr.deletedAt IS NULL
            """)
    List<StudentCourseRecord> findActiveByStudentIdAndTeacherId(@Param("studentId") UUID studentId,
                                                                @Param("teacherId") UUID teacherId);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            WHERE scr.studentMemberId = :studentId
              AND c.teacherMemberId IN :teacherIds
              AND scr.deletedAt IS NULL
            """)
    List<StudentCourseRecord> findActiveByStudentIdAndTeacherIds(@Param("studentId") UUID studentId,
                                                                 @Param("teacherIds") List<UUID> teacherIds);

    Optional<StudentCourseRecord> findByStudentMemberIdAndCourseIdAndDeletedAtIsNull(UUID studentMemberId,
                                                                                      UUID courseId);

    Optional<StudentCourseRecord> findByStudentMemberIdAndCourseId(UUID studentMemberId,
                                                                    UUID courseId);

    List<StudentCourseRecord> findByStudentMemberIdAndDeletedAtIsNull(UUID studentMemberId);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            WHERE scr.studentMemberId = :studentId
              AND c.teacherMemberId = :teacherId
            """)
    List<StudentCourseRecord> findByStudentMemberIdAndTeacherMemberId(@Param("studentId") UUID studentId,
                                                                      @Param("teacherId") UUID teacherId);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            WHERE scr.studentMemberId = :studentId
              AND c.teacherMemberId IN :teacherIds
            """)
    List<StudentCourseRecord> findByStudentMemberIdAndTeacherMemberIdIn(@Param("studentId") UUID studentId,
                                                                        @Param("teacherIds") List<UUID> teacherIds);

    @Query("""
            SELECT COUNT(scr)
            FROM StudentCourseRecord scr
            JOIN Course c ON c.id = scr.courseId
            WHERE scr.studentMemberId = :studentId
              AND c.teacherMemberId = :teacherId
              AND c.branchId = :branchId
              AND scr.deletedAt IS NULL
            """)
    long countActiveByStudentAndTeacherAndBranch(@Param("studentId") UUID studentId,
                                                 @Param("teacherId") UUID teacherId,
                                                 @Param("branchId") UUID branchId);

    @Query("""
            SELECT scr
            FROM StudentCourseRecord scr
            WHERE scr.courseId = :courseId
              AND scr.studentMemberId IN :studentIds
              AND scr.deletedAt IS NULL
            """)
    List<StudentCourseRecord> findActiveByCourseIdAndStudentIds(@Param("courseId") UUID courseId,
                                                                @Param("studentIds") List<UUID> studentIds);
}
