package com.classhub.global.init.data;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.init.seeds.InitTeacherBranchAssignments;
import com.classhub.global.init.seeds.InitTeacherBranchAssignments.TeacherBranchSeed;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test"})
public class TeacherBranchAssignmentInitData extends BaseInitData {

    private final TeacherBranchAssignmentRepository assignmentRepository;
    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    public TeacherBranchAssignmentInitData(TeacherBranchAssignmentRepository assignmentRepository,
                                           MemberRepository memberRepository,
                                           CompanyRepository companyRepository,
                                           BranchRepository branchRepository) {
        super("season2-teacher-branch-assignment-seed", 60);
        this.assignmentRepository = assignmentRepository;
        this.memberRepository = memberRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (TeacherBranchSeed seed : InitTeacherBranchAssignments.seeds()) {
            Optional<Member> teacher = memberRepository.findByEmail(seed.teacherEmail());
            if (teacher.isEmpty()) {
                log.warn("Skipping teacher-branch seed. Teacher not found for email={}", seed.teacherEmail());
                continue;
            }
            Optional<Company> company = companyRepository.findByName(seed.companyName());
            if (company.isEmpty()) {
                log.warn("Skipping teacher-branch seed. Company not found for name={}", seed.companyName());
                continue;
            }
            for (String branchName : seed.branchNames()) {
                Optional<Branch> branch = branchRepository.findByCompanyIdAndName(company.get().getId(), branchName);
                if (branch.isEmpty()) {
                    log.warn("Skipping teacher-branch seed. Branch not found for name={}", branchName);
                    continue;
                }
                upsertAssignment(teacher.get().getId(), branch.get().getId(), seed, force);
            }
        }
    }

    private void upsertAssignment(java.util.UUID teacherId,
                                  java.util.UUID branchId,
                                  TeacherBranchSeed seed,
                                  boolean force) {
        Optional<TeacherBranchAssignment> existing = assignmentRepository
                .findByTeacherMemberIdAndBranchId(teacherId, branchId);
        if (existing.isPresent()) {
            TeacherBranchAssignment assignment = existing.get();
            if (force) {
                assignment.changeRole(seed.role());
                assignment.enable();
            }
            return;
        }

        TeacherBranchAssignment assignment = TeacherBranchAssignment.create(
                teacherId,
                branchId,
                seed.role()
        );
        assignmentRepository.save(assignment);
    }
}
