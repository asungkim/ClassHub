package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentCourseRecordRepository extends JpaRepository<StudentCourseRecord, UUID> {

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
}
