package com.classhub.domain.company.company.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "company",
        indexes = {
                @Index(name = "idx_company_verified_status", columnList = "verified_status"),
                @Index(name = "idx_company_creator", columnList = "creator_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", nullable = false, length = 20)
    private VerifiedStatus verifiedStatus;

    @Column(name = "creator_member_id", columnDefinition = "BINARY(16)")
    private UUID creatorMemberId;

    @Builder
    private Company(String name,
                    String description,
                    CompanyType type,
                    VerifiedStatus verifiedStatus,
                    UUID creatorMemberId) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.description = description;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.verifiedStatus = verifiedStatus == null ? VerifiedStatus.VERIFIED : verifiedStatus;
        this.creatorMemberId = creatorMemberId;
    }

    public static Company create(String name,
                                 String description,
                                 CompanyType type,
                                 VerifiedStatus verifiedStatus,
                                 UUID creatorMemberId) {
        return Company.builder()
                .name(name)
                .description(description)
                .type(type)
                .verifiedStatus(verifiedStatus)
                .creatorMemberId(creatorMemberId)
                .build();
    }

    public void verify() {
        this.verifiedStatus = VerifiedStatus.VERIFIED;
    }

    public void markUnverified() {
        this.verifiedStatus = VerifiedStatus.UNVERIFIED;
    }
}
