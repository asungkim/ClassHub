package com.classhub.domain.feedback.repository;

import com.classhub.domain.feedback.model.Feedback;
import com.classhub.domain.feedback.model.FeedbackStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    Page<Feedback> findAllByStatus(FeedbackStatus status, Pageable pageable);

    Page<Feedback> findAllByMemberId(UUID memberId, Pageable pageable);

    Page<Feedback> findAllByMemberIdAndStatus(UUID memberId, FeedbackStatus status, Pageable pageable);
}
