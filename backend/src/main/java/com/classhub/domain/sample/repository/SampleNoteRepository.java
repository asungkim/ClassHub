package com.classhub.domain.sample.repository;

import com.classhub.domain.sample.model.SampleNote;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleNoteRepository extends JpaRepository<SampleNote, UUID> {
}
