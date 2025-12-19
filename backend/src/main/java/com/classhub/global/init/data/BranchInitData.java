package com.classhub.global.init.data;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.init.seeds.InitBranches;
import com.classhub.global.init.seeds.InitBranches.BranchSeed;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test", "dev", "prod"})
public class BranchInitData extends BaseInitData {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final MemberRepository memberRepository;

    public BranchInitData(CompanyRepository companyRepository,
                          BranchRepository branchRepository,
                          MemberRepository memberRepository) {
        super("season2-branch-seed", 210);
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (BranchSeed seed : InitBranches.seeds()) {
            Optional<Company> company = companyRepository.findByName(seed.companyName());
            if (company.isEmpty()) {
                log.warn("Skipping branch seed. Company not found for name={}", seed.companyName());
                continue;
            }
            upsertBranch(company.get(), seed, force);
        }
    }

    private void upsertBranch(Company company, BranchSeed seed, boolean force) {
        UUID creatorId = resolveMemberId(seed.creatorEmail());
        Optional<Branch> existing = branchRepository.findByCompanyIdAndName(company.getId(), seed.name());
        if (existing.isPresent()) {
            if (force) {
                existing.get().applySeed(company.getId(), seed.name(), creatorId, seed.verifiedStatus());
            }
            return;
        }

        Branch branch = Branch.create(
                company.getId(),
                seed.name(),
                creatorId,
                seed.verifiedStatus()
        );
        branchRepository.save(branch);
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
