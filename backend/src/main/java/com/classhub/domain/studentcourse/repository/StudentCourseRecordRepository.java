package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCourseRecordRepository extends JpaRepository<StudentCourseRecord, UUID> {
}
