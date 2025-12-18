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

    @Column(nullable = false, length = 40)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

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

    public void changePhoneNumber(String phoneNumber) {
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    public void activate() {
        restore();
    }

    public void deactivate() {
        delete();
    }
}
