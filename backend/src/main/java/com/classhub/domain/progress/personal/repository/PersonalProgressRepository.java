package com.classhub.domain.progress.personal.repository;

import com.classhub.domain.progress.personal.model.PersonalProgress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalProgressRepository extends JpaRepository<PersonalProgress, UUID> {
}
