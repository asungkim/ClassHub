package com.classhub.domain.course.application;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseViewAssembler {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    public CourseContext buildContext(Collection<Course> courses) {
        Map<UUID, Branch> branchMap = loadBranchMap(courses);
        Map<UUID, Company> companyMap = loadCompanyMap(branchMap);
        Map<UUID, UUID> branchCompanyMap = branchMap.values().stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getCompanyId));
        return new CourseContext(branchMap, companyMap, branchCompanyMap);
    }

    public CourseResponse toCourseResponse(Course course, CourseContext context) {
        Branch branch = context.branches().get(course.getBranchId());
        if (branch == null || branch.isDeleted()) {
            throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
        }
        UUID companyId = context.branchCompanyMap().get(branch.getId());
        Company company = context.companies().get(companyId);
        if (company == null || company.isDeleted()) {
            throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
        }
        return new CourseResponse(
                course.getId(),
                branch.getId(),
                branch.getName(),
                company.getId(),
                company.getName(),
                course.getName(),
                course.getDescription(),
                course.getStartDate(),
                course.getEndDate(),
                !course.isDeleted(),
                toScheduleResponses(course.getSchedules())
        );
    }

    private Map<UUID, Branch> loadBranchMap(Collection<Course> courses) {
        List<UUID> branchIds = courses.stream()
                .map(Course::getBranchId)
                .distinct()
                .toList();
        List<Branch> branches = branchRepository.findAllById(branchIds);
        if (branches.size() < branchIds.size()) {
            throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
        }
        return branches.stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
    }

    private Map<UUID, Company> loadCompanyMap(Map<UUID, Branch> branchMap) {
        List<UUID> companyIds = branchMap.values().stream()
                .map(Branch::getCompanyId)
                .distinct()
                .toList();
        List<Company> companies = companyRepository.findAllById(companyIds);
        if (companies.size() < companyIds.size()) {
            throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
        }
        return companies.stream()
                .collect(Collectors.toMap(Company::getId, company -> company));
    }

    private List<CourseScheduleResponse> toScheduleResponses(Collection<Course.CourseSchedule> schedules) {
        return schedules.stream()
                .sorted(Comparator
                        .comparing((Course.CourseSchedule s) -> s.getDayOfWeek().getValue())
                        .thenComparing(Course.CourseSchedule::getStartTime))
                .map(schedule -> new CourseScheduleResponse(
                        schedule.getDayOfWeek(),
                        schedule.getStartTime(),
                        schedule.getEndTime()
                ))
                .toList();
    }

    public record CourseContext(
            Map<UUID, Branch> branches,
            Map<UUID, Company> companies,
            Map<UUID, UUID> branchCompanyMap
    ) {
    }
}
