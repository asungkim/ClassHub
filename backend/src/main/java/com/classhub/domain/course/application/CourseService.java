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
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.course.validator.CoursePeriodValidator;
import com.classhub.domain.course.validator.CourseScheduleValidator;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
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
    private final CourseViewAssembler courseViewAssembler;

    public CourseResponse createCourse(UUID teacherId, CourseCreateRequest request) {
        Branch branch = requireActiveBranch(request.branchId());
        Company company = requireCompany(branch.getCompanyId());
        teacherBranchAssignmentRepository
                .findByTeacherMemberIdAndBranchId(teacherId, request.branchId())
                .filter(TeacherBranchAssignment::isActive)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_FORBIDDEN));

        CoursePeriodValidator.validate(request.startDate(), request.endDate());
        Set<Course.CourseSchedule> schedules = toSchedules(request.schedules());
        ensureNoScheduleOverlap(
                teacherId,
                request.startDate(),
                request.endDate(),
                schedules,
                null
        );

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
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(result.getContent());
        Page<CourseResponse> mapped = result.map(course -> courseViewAssembler.toCourseResponse(course, context));
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesWithinPeriod(UUID teacherId,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {
        CoursePeriodValidator.validate(startDate, endDate);
        List<Course> courses = courseRepository.findCoursesWithinPeriod(teacherId, startDate, endDate);
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(courses);
        return courses.stream()
                .map(course -> courseViewAssembler.toCourseResponse(course, context))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID teacherId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(RsCode.COURSE_NOT_FOUND));
        if (!Objects.equals(course.getTeacherMemberId(), teacherId)) {
            throw new BusinessException(RsCode.COURSE_FORBIDDEN);
        }
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(course));
        return courseViewAssembler.toCourseResponse(course, context);
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
        boolean scheduleChanged = request.schedules() != null;
        boolean periodChanged = request.startDate() != null || request.endDate() != null;
        Set<Course.CourseSchedule> updatedSchedules = scheduleChanged
                ? toSchedules(request.schedules())
                : course.getSchedules();
        if (scheduleChanged || periodChanged) {
            ensureNoScheduleOverlap(
                    teacherId,
                    startDate,
                    endDate,
                    updatedSchedules,
                    course.getId()
            );
        }
        if (request.schedules() != null) {
            course.replaceSchedules(updatedSchedules);
        }
        Course saved = courseRepository.save(course);
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(saved));
        return courseViewAssembler.toCourseResponse(saved, context);
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
        CourseViewAssembler.CourseContext context = courseViewAssembler.buildContext(List.of(saved));
        return courseViewAssembler.toCourseResponse(saved, context);
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CourseResponse toCourseResponse(Course course, Branch branch, Company company) {
        CourseViewAssembler.CourseContext context = new CourseViewAssembler.CourseContext(
                Map.of(branch.getId(), branch),
                Map.of(company.getId(), company),
                Map.of(branch.getId(), company.getId())
        );
        return courseViewAssembler.toCourseResponse(course, context);
    }

    private void ensureNoScheduleOverlap(UUID teacherId,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         Set<Course.CourseSchedule> schedules,
                                         UUID excludeId) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }
        List<Course> overlaps = courseRepository.findOverlappingCourses(
                teacherId,
                startDate,
                endDate,
                excludeId
        );
        for (Course course : overlaps) {
            for (Course.CourseSchedule existing : course.getSchedules()) {
                for (Course.CourseSchedule candidate : schedules) {
                    if (isScheduleOverlapping(existing, candidate)) {
                        throw new BusinessException(RsCode.COURSE_SCHEDULE_OVERLAP);
                    }
                }
            }
        }
    }

    private boolean isScheduleOverlapping(Course.CourseSchedule existing, Course.CourseSchedule candidate) {
        if (existing.getDayOfWeek() != candidate.getDayOfWeek()) {
            return false;
        }
        return existing.getStartTime().isBefore(candidate.getEndTime())
                && candidate.getStartTime().isBefore(existing.getEndTime());
    }
}
