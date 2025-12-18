package com.classhub.domain.worklog.repository;

import com.classhub.domain.worklog.model.WorkLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogRepository extends JpaRepository<WorkLog, UUID> {
}
