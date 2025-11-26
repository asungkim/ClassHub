package com.classhub.global.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.sample.model.SampleNote;
import com.classhub.domain.sample.repository.SampleNoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BaseEntityAuditingTest {

    @Autowired
    private SampleNoteRepository sampleNoteRepository;

    @Test
    void auditingColumnsAreFilledAutomatically() {
        SampleNote saved = sampleNoteRepository.save(SampleNote.create("audit test"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
