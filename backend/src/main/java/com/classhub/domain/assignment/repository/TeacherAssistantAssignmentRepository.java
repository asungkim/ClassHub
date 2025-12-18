package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAssistantAssignmentRepository
        extends JpaRepository<TeacherAssistantAssignment, UUID> {
}
