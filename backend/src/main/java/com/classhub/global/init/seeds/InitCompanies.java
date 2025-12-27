package com.classhub.global.init.seeds;

import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import java.util.List;

/**
 * Default Company seed definitions used for initial data bootstrapping.
 */
public final class InitCompanies {

    private InitCompanies() {
    }

    public static List<CompanySeed> seeds() {
        return List.of(
                new CompanySeed(
                        "러셀",
                        "러셀 러닝센터",
                        CompanyType.ACADEMY,
                        VerifiedStatus.VERIFIED,
                        null
                ),
                new CompanySeed(
                        "두각",
                        "두각 MJELITE 캠퍼스",
                        CompanyType.ACADEMY,
                        VerifiedStatus.VERIFIED,
                        null
                ),
                new CompanySeed(
                        "시대인재",
                        "시대인재 학원",
                        CompanyType.ACADEMY,
                        VerifiedStatus.VERIFIED,
                        null
                ),
                new CompanySeed(
                        "미래탐구",
                        "미래탐구 네트워크",
                        CompanyType.ACADEMY,
                        VerifiedStatus.VERIFIED,
                        null
                )
        );
    }

    public record CompanySeed(
            String name,
            String description,
            CompanyType type,
            VerifiedStatus verifiedStatus,
            String creatorEmail
    ) {
    }
}
