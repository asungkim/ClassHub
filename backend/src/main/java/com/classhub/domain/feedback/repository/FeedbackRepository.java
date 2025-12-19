package com.classhub.domain.feedback.repository;

import com.classhub.domain.feedback.model.Feedback;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
