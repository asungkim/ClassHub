package com.classhub.domain.member.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_teacher", columnList = "teacher_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Member extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "teacher_id", columnDefinition = "BINARY(16)")
    private UUID teacherId;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeName(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public void changeRole(MemberRole role) {
        if (role != null) {
            this.role = role;
        }
    }

    public void assignTeacher(UUID teacherId) {
        this.teacherId = teacherId;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
