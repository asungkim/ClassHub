package com.classhub.domain.company.branch.model;

import com.classhub.domain.company.company.model.VerifiedStatus;
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
        name = "branch",
        indexes = {
                @Index(name = "idx_branch_company", columnList = "company_id"),
                @Index(name = "idx_branch_verified_status", columnList = "verified_status"),
                @Index(name = "idx_branch_creator", columnList = "creator_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch extends BaseEntity {

    @Column(name = "company_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "creator_member_id", columnDefinition = "BINARY(16)")
    private UUID creatorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", nullable = false, length = 20)
    private VerifiedStatus verifiedStatus;

    @Builder
    private Branch(UUID companyId,
                   String name,
                   UUID creatorMemberId,
                   VerifiedStatus verifiedStatus) {
        this.companyId = Objects.requireNonNull(companyId, "companyId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.creatorMemberId = creatorMemberId;
        this.verifiedStatus = verifiedStatus == null ? VerifiedStatus.VERIFIED : verifiedStatus;
    }

    public static Branch create(UUID companyId,
                                String name,
                                UUID creatorMemberId,
                                VerifiedStatus verifiedStatus) {
        return Branch.builder()
                .companyId(companyId)
                .name(name)
                .creatorMemberId(creatorMemberId)
                .verifiedStatus(verifiedStatus)
                .build();
    }

    public void verify() {
        this.verifiedStatus = VerifiedStatus.VERIFIED;
    }

    public void markUnverified() {
        this.verifiedStatus = VerifiedStatus.UNVERIFIED;
    }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName, "name must not be null").trim();
    }

    public boolean isOwnedBy(UUID memberId) {
        return memberId != null && memberId.equals(this.creatorMemberId);
    }

    public void applySeed(UUID companyId,
                          String name,
                          UUID creatorMemberId,
                          VerifiedStatus verifiedStatus) {
        this.companyId = Objects.requireNonNull(companyId, "companyId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.creatorMemberId = creatorMemberId;
        this.verifiedStatus = verifiedStatus == null ? VerifiedStatus.VERIFIED : verifiedStatus;
    }
}
