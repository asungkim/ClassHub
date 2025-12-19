package com.classhub.domain.course.application;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.course.dto.CourseStatusFilter;
import com.classhub.domain.course.dto.request.CourseCreateRequest;
import com.classhub.domain.course.dto.request.CourseScheduleRequest;
import com.classhub.domain.course.dto.request.CourseStatusUpdateRequest;
import com.classhub.domain.course.dto.request.CourseUpdateRequest;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.course.validator.CoursePeriodValidator;
import com.classhub.domain.course.validator.CourseScheduleValidator;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    public CourseResponse createCourse(UUID teacherId, CourseCreateRequest request) {
        Branch branch = requireActiveBranch(request.branchId());
        Company company = requireCompany(branch.getCompanyId());
        teacherBranchAssignmentRepository
                .findByTeacherMemberIdAndBranchId(teacherId, request.branchId())
                .filter(TeacherBranchAssignment::isActive)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_FORBIDDEN));

        CoursePeriodValidator.validate(request.startDate(), request.endDate());
        Set<Course.CourseSchedule> schedules = toSchedules(request.schedules());

        Course course = Course.create(
                branch.getId(),
                teacherId,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate(),
                schedules
        );
        Course saved = courseRepository.save(course);
        return toCourseResponse(saved, branch, company);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCourses(UUID teacherId,
                                                   CourseStatusFilter status,
                                                   UUID branchId,
                                                   String keyword,
                                                   int page,
                                                   int size) {
        Page<Course> result = courseRepository.searchCourses(
                teacherId,
                branchId,
                status,
                normalizeKeyword(keyword),
                PageRequest.of(page, size)
        );
        CourseContext context = buildContext(result.getContent());
        Page<CourseResponse> mapped = result.map(course -> toCourseResponse(
                course,
                context.branches().get(course.getBranchId()),
                context.companies().get(context.branchCompanyMap().get(course.getBranchId()))
        ));
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesWithinPeriod(UUID teacherId,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {
        CoursePeriodValidator.validate(startDate, endDate);
        List<Course> courses = courseRepository.findCoursesWithinPeriod(teacherId, startDate, endDate);
        CourseContext context = buildContext(courses);
        return courses.stream()
                .map(course -> toCourseResponse(
                        course,
                        context.branches().get(course.getBranchId()),
                        context.companies().get(context.branchCompanyMap().get(course.getBranchId()))
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID teacherId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (!Objects.equals(course.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        CourseContext context = buildContext(List.of(course));
        Branch branch = context.branches().get(course.getBranchId());
        UUID companyId = context.branchCompanyMap().get(course.getBranchId());
        Company company = context.companies().get(companyId);
        return toCourseResponse(course, branch, company);
    }

    public CourseResponse updateCourse(UUID teacherId,
                                       UUID courseId,
                                       CourseUpdateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (!Objects.equals(course.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        LocalDate startDate = request.startDate() != null ? request.startDate() : course.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : course.getEndDate();
        CoursePeriodValidator.validate(startDate, endDate);
        course.updateInfo(
                request.name(),
                startDate,
                endDate
        );
        course.updateDescription(request.description());
        if (request.schedules() != null) {
            course.replaceSchedules(toSchedules(request.schedules()));
        }
        Course saved = courseRepository.save(course);
        CourseContext context = buildContext(List.of(saved));
        Branch branch = context.branches().get(saved.getBranchId());
        UUID companyId = context.branchCompanyMap().get(saved.getBranchId());
        Company company = context.companies().get(companyId);
        return toCourseResponse(saved, branch, company);
    }

    public CourseResponse updateCourseStatus(UUID teacherId,
                                             UUID courseId,
                                             CourseStatusUpdateRequest request) {
        if (request.enabled() == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (!Objects.equals(course.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        if (request.enabled() && course.isDeleted()) {
            course.activate();
        } else if (!request.enabled() && !course.isDeleted()) {
            course.deactivate();
        }
        Course saved = courseRepository.save(course);
        CourseContext context = buildContext(List.of(saved));
        Branch branch = context.branches().get(saved.getBranchId());
        UUID companyId = context.branchCompanyMap().get(saved.getBranchId());
        Company company = context.companies().get(companyId);
        return toCourseResponse(saved, branch, company);
    }

    private Branch requireActiveBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(RsCode.BRANCH_NOT_FOUND));
        if (branch.isDeleted() || branch.getVerifiedStatus() != VerifiedStatus.VERIFIED) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        return branch;
    }

    private Company requireCompany(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(RsCode.COMPANY_NOT_FOUND));
        if (company.isDeleted()) {
            throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
        }
        return company;
    }

    private Set<Course.CourseSchedule> toSchedules(List<CourseScheduleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        List<CourseScheduleValidator.ScheduleInput> inputs = requests.stream()
                .map(req -> new CourseScheduleValidator.ScheduleInput(req.dayOfWeek(), req.startTime(), req.endTime()))
                .toList();
        CourseScheduleValidator.validate(inputs);
        return requests.stream()
                .map(req -> new Course.CourseSchedule(req.dayOfWeek(), req.startTime(), req.endTime()))
                .collect(Collectors.toSet());
    }

    private CourseResponse toCourseResponse(Course course, Branch branch, Company company) {
        if (branch == null || branch.isDeleted()) {
            throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
        }
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

    private List<CourseScheduleResponse> toScheduleResponses(Set<Course.CourseSchedule> schedules) {
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

    private CourseContext buildContext(Collection<Course> courses) {
        Set<UUID> branchIds = courses.stream()
                .map(Course::getBranchId)
                .collect(Collectors.toSet());
        List<Branch> branches = branchRepository.findAllById(branchIds);
        Map<UUID, Branch> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
        if (branchMap.size() < branchIds.size()) {
            throw new BusinessException(RsCode.BRANCH_NOT_FOUND);
        }
        Set<UUID> companyIds = branchMap.values().stream()
                .map(Branch::getCompanyId)
                .collect(Collectors.toSet());
        List<Company> companies = companyRepository.findAllById(companyIds);
        Map<UUID, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(Company::getId, company -> company));
        if (companyMap.size() < companyIds.size()) {
            throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
        }
        Map<UUID, UUID> branchCompanyMap = branchMap.values().stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getCompanyId));
        return new CourseContext(branchMap, companyMap, branchCompanyMap);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record CourseContext(
            Map<UUID, Branch> branches,
            Map<UUID, Company> companies,
            Map<UUID, UUID> branchCompanyMap
    ) {
    }
}
