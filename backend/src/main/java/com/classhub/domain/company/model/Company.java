package com.classhub.domain.company.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "company", indexes = {
        @Index(name = "idx_company_status", columnList = "status"),
        @Index(name = "idx_company_creator", columnList = "creator_member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Company extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType type;  // INDIVIDUAL, ACADEMY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status;  // UNVERIFIED, VERIFIED, REJECTED

    @Column(name = "creator_member_id")
    private UUID creatorMemberId;

    @Column(nullable = false)
    private boolean isActive = true;
}
