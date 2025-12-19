package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherBranchAssignmentRepository extends JpaRepository<TeacherBranchAssignment, UUID> {
}
