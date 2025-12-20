import type { components } from "@/types/openapi";

export type TeacherBranchAssignment = components["schemas"]["TeacherBranchAssignmentResponse"];
export type TeacherBranchAssignmentCreateRequest = components["schemas"]["TeacherBranchAssignmentCreateRequest"];
export type TeacherBranchAssignmentStatusUpdateRequest = components["schemas"]["TeacherBranchAssignmentStatusUpdateRequest"];
export type TeacherBranchAssignmentPage = components["schemas"]["PageResponseTeacherBranchAssignmentResponse"];

export type TeacherBranchAssignmentListResponse = components["schemas"]["RsDataPageResponseTeacherBranchAssignmentResponse"];
export type TeacherBranchAssignmentResponseData = Required<TeacherBranchAssignmentListResponse>["data"];

export type CompanyResponse = components["schemas"]["CompanyResponse"];
export type BranchResponse = components["schemas"]["BranchResponse"];

export type CompanyPage = components["schemas"]["PageResponseCompanyResponse"];
export type BranchPage = components["schemas"]["PageResponseBranchResponse"];

export type CompanyListResponse = components["schemas"]["RsDataPageResponseCompanyResponse"];
export type BranchListResponse = components["schemas"]["RsDataPageResponseBranchResponse"];

export type CompanyVerifiedStatusRequest = components["schemas"]["CompanyVerifiedStatusRequest"];
export type BranchVerifiedStatusRequest = components["schemas"]["BranchVerifiedStatusRequest"];

export type TeacherAssignmentFilter = "ACTIVE" | "INACTIVE" | "ALL";
export type VerificationFilter = "ALL" | "VERIFIED" | "UNVERIFIED";

export type CourseResponse = components["schemas"]["CourseResponse"];
export type CourseListResponse = components["schemas"]["RsDataPageResponseCourseResponse"];
export type CourseCalendarResponse = components["schemas"]["RsDataListCourseResponse"];
export type CourseStatusFilter = "ACTIVE" | "INACTIVE" | "ALL";
export type CourseCreateRequest = components["schemas"]["CourseCreateRequest"];
export type CourseUpdateRequest = components["schemas"]["CourseUpdateRequest"];
export type CourseStatusUpdateRequest = components["schemas"]["CourseStatusUpdateRequest"];
export type CourseScheduleRequest = components["schemas"]["CourseScheduleRequest"];
export type CourseWithTeacherResponse = components["schemas"]["CourseWithTeacherResponse"];
export type PublicCourseResponse = components["schemas"]["PublicCourseResponse"];
