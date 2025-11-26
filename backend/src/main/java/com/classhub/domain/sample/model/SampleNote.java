package com.classhub.domain.sample.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SampleNote extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String content;

    private SampleNote(String content) {
        this.content = content;
    }

    public static SampleNote create(String content) {
        return new SampleNote(content);
    }
}
