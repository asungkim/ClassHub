package com.classhub.global.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.model.TeacherStudentRequestStatus;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.assignment.repository.StudentTeacherRequestRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
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
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    @Autowired
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentTeacherRequestRepository studentTeacherRequestRepository;

    @Test
    void membersAndStudentInfosAreSeeded() {
        // admin + 2 teachers + 4 assistants + 100 students
        assertThat(memberRepository.count()).isGreaterThanOrEqualTo(InitMembers.seeds().size());

        Member superAdmin = memberRepository.findByEmail("ad@n.com").orElseThrow();
        assertThat(superAdmin.getRole()).isEqualTo(MemberRole.SUPER_ADMIN);

        Member teacher1 = memberRepository.findByEmail("te1@n.com").orElseThrow();
        Member teacher2 = memberRepository.findByEmail("te2@n.com").orElseThrow();
        assertThat(teacher1.getRole()).isEqualTo(MemberRole.TEACHER);
        assertThat(teacher2.getRole()).isEqualTo(MemberRole.TEACHER);

        for (int i = 1; i <= 4; i += 1) {
            Member assistant = memberRepository.findByEmail("as" + i + "@n.com").orElseThrow();
            assertThat(assistant.getRole()).isEqualTo(MemberRole.ASSISTANT);
        }

        List<String> sampleStudents = List.of("st1@n.com", "st50@n.com", "st100@n.com");
        for (String email : sampleStudents) {
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
                "러셀", 16,
                "두각", 15,
                "시대인재", 5,
                "미래탐구", 17
        );

        expectedBranchCounts.forEach((companyName, expectedCount) -> {
            Company company = companyRepository.findByName(companyName).orElseThrow();
            assertThat(company.getType()).isEqualTo(CompanyType.ACADEMY);

            List<Branch> branches = branchRepository.findByCompanyId(company.getId());
            assertThat(branches).hasSize(expectedCount);
        });

        assertUnverifiedBranch("러셀", "전주");
        assertUnverifiedBranch("두각", "분당");
        assertUnverifiedBranch("미래탐구", "전주");
    }

    @Test
    @Transactional
    void assignmentsCoursesSlotsAndRequestsAreSeeded() {
        Member teacher1 = memberRepository.findByEmail("te1@n.com").orElseThrow();
        Member teacher2 = memberRepository.findByEmail("te2@n.com").orElseThrow();

        assertTeacherAssignments(teacher1.getId());
        assertTeacherAssignments(teacher2.getId());

        assertCoursesForTeacher(teacher1.getId());
        assertCoursesForTeacher(teacher2.getId());

        assertStudentTeacherRequests(teacher1.getId(), 75);
        assertStudentTeacherRequests(teacher2.getId(), 75);
    }

    private void assertUnverifiedBranch(String companyName, String branchName) {
        Company company = companyRepository.findByName(companyName).orElseThrow();
        Branch branch = branchRepository.findByCompanyIdAndName(company.getId(), branchName).orElseThrow();
        assertThat(branch.getVerifiedStatus()).isEqualTo(VerifiedStatus.UNVERIFIED);
    }

    private void assertTeacherAssignments(UUID teacherId) {
        Page<TeacherBranchAssignment> branchAssignments = teacherBranchAssignmentRepository
                .findByTeacherMemberIdAndDeletedAtIsNull(teacherId, Pageable.unpaged());
        assertThat(branchAssignments.getContent()).hasSize(2);

        List<TeacherAssistantAssignment> assistantAssignments = teacherAssistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdIn(teacherId, loadAssistantIds());
        assertThat(assistantAssignments).hasSize(4);
    }

    private void assertCoursesForTeacher(UUID teacherId) {
        Page<Course> courses = courseRepository.searchCoursesInternal(
                teacherId,
                null,
                false,
                false,
                null,
                Pageable.unpaged()
        );
        assertThat(courses.getContent()).hasSize(4);
        courses.forEach(course -> assertThat(course.getSchedules()).hasSize(2));
    }

    private void assertStudentTeacherRequests(UUID teacherId, int expectedCount) {
        long count = studentTeacherRequestRepository.findAll().stream()
                .filter(request -> request.getTeacherMemberId().equals(teacherId))
                .filter(request -> request.getStatus() == TeacherStudentRequestStatus.PENDING)
                .count();
        assertThat(count).isEqualTo(expectedCount);
    }

    private List<UUID> loadAssistantIds() {
        return List.of("as1@n.com", "as2@n.com", "as3@n.com", "as4@n.com").stream()
                .map(email -> memberRepository.findByEmail(email).orElseThrow().getId())
                .toList();
    }
}
