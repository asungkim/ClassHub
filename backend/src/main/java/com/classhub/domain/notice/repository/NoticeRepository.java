package com.classhub.domain.notice.repository;

import com.classhub.domain.notice.model.Notice;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
}
