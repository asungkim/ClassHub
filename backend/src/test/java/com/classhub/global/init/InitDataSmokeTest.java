package com.classhub.global.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.init.seeds.InitBranches;
import com.classhub.global.init.seeds.InitCompanies;
import com.classhub.global.init.seeds.InitMembers;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InitDataSmokeTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private StudentInfoRepository studentInfoRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private BranchRepository branchRepository;

    @Test
    void membersAndStudentInfosAreSeeded() {
        // super admin + 2 teachers + 2 assistants + 4 students
        assertThat(memberRepository.count()).isGreaterThanOrEqualTo(InitMembers.seeds().size());

        Member superAdmin = memberRepository.findByEmail("superadmin@classhub.dev").orElseThrow();
        assertThat(superAdmin.getRole()).isEqualTo(MemberRole.SUPER_ADMIN);

        List<String> studentEmails = List.of(
                "student.jaekyung@classhub.dev",
                "student.arin@classhub.dev",
                "student.donghyuk@classhub.dev",
                "student.sumin@classhub.dev"
        );
        for (String email : studentEmails) {
            Member member = memberRepository.findByEmail(email).orElseThrow();
            StudentInfo info = studentInfoRepository.findByMemberId(member.getId()).orElse(null);
            assertThat(info)
                    .withFailMessage("StudentInfo should exist for %s", email)
                    .isNotNull();
        }
    }

    @Test
    void companiesAndBranchesAreSeeded() {
        assertThat(companyRepository.count()).isGreaterThanOrEqualTo(InitCompanies.seeds().size());
        assertThat(branchRepository.count()).isEqualTo(InitBranches.seeds().size());

        Map<String, Integer> expectedBranchCounts = Map.of(
                "Alice Private Lab", 1,
                "러셀", 16,
                "두각", 15,
                "시대인재", 5,
                "미래탐구", 17
        );

        expectedBranchCounts.forEach((companyName, expectedCount) -> {
            Company company = companyRepository.findByName(companyName).orElseThrow();
            CompanyType expectedType = "Alice Private Lab".equals(companyName) ? CompanyType.INDIVIDUAL : CompanyType.ACADEMY;
            assertThat(company.getType()).isEqualTo(expectedType);

            List<Branch> branches = branchRepository.findByCompanyId(company.getId());
            assertThat(branches).hasSize(expectedCount);
        });

        assertUnverifiedBranch("러셀", "전주");
        assertUnverifiedBranch("두각", "분당");
        assertUnverifiedBranch("미래탐구", "전주");
    }

    private void assertUnverifiedBranch(String companyName, String branchName) {
        Company company = companyRepository.findByName(companyName).orElseThrow();
        Branch branch = branchRepository.findByCompanyIdAndName(company.getId(), branchName).orElseThrow();
        assertThat(branch.getVerifiedStatus()).isEqualTo(VerifiedStatus.UNVERIFIED);
    }
}
