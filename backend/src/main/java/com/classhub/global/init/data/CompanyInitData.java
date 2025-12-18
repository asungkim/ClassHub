package com.classhub.global.init.data;

import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.init.seeds.InitCompanies;
import com.classhub.global.init.seeds.InitCompanies.CompanySeed;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test", "dev", "prod"})
public class CompanyInitData extends BaseInitData {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;

    public CompanyInitData(CompanyRepository companyRepository,
                           MemberRepository memberRepository) {
        super("season2-company-seed", 200);
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (CompanySeed seed : InitCompanies.seeds()) {
            upsertCompany(seed, force);
        }
    }

    private void upsertCompany(CompanySeed seed, boolean force) {
        UUID creatorMemberId = resolveMemberId(seed.creatorEmail());
        Optional<Company> existing = companyRepository.findByName(seed.name());
        if (existing.isPresent()) {
            if (force) {
                existing.get()
                        .applySeed(seed.name(), seed.description(), seed.type(), seed.verifiedStatus(), creatorMemberId);
            }
            return;
        }

        Company company = Company.create(
                seed.name(),
                seed.description(),
                seed.type(),
                seed.verifiedStatus(),
                creatorMemberId
        );
        companyRepository.save(company);
    }

    private UUID resolveMemberId(String email) {
        if (email == null) {
            return null;
        }
        return memberRepository.findByEmail(email)
                .map(Member::getId)
                .orElseGet(() -> {
                    log.warn("Creator member not found for email={}", email);
                    return null;
                });
    }
}
