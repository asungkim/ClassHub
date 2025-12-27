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
export type CourseStudentResponse = components["schemas"]["CourseStudentResponse"];
export type TeacherBranchSummary = components["schemas"]["TeacherBranchSummary"];
export type TeacherSearchResponse = components["schemas"]["TeacherSearchResponse"];
export type TeacherSearchPage = components["schemas"]["PageResponseTeacherSearchResponse"];

export type StudentTeacherRequestCreateRequest = components["schemas"]["StudentTeacherRequestCreateRequest"];
export type StudentTeacherRequestResponse = components["schemas"]["StudentTeacherRequestResponse"];
export type StudentTeacherRequestPage = components["schemas"]["PageResponseStudentTeacherRequestResponse"];
export type StudentTeacherRequestStatus = NonNullable<StudentTeacherRequestResponse["status"]>;
export type AssistantAssignmentResponse = components["schemas"]["AssistantAssignmentResponse"];
export type AssistantAssignmentPage = components["schemas"]["PageResponseAssistantAssignmentResponse"];
export type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];

export type StudentCourseDetailResponse = components["schemas"]["StudentCourseDetailResponse"];
export type StudentCourseAssignmentCreateRequest = components["schemas"]["StudentCourseAssignmentCreateRequest"];
export type StudentCourseAssignmentResponse = components["schemas"]["StudentCourseAssignmentResponse"];
export type StudentCourseRecordUpdateRequest = components["schemas"]["StudentCourseRecordUpdateRequest"];
export type StudentMyCourseResponse = components["schemas"]["StudentMyCourseResponse"];
export type StudentSummaryResponse = components["schemas"]["StudentSummaryResponse"];
export type TeacherStudentCourseResponse = components["schemas"]["TeacherStudentCourseResponse"];
export type TeacherStudentDetailResponse = components["schemas"]["TeacherStudentDetailResponse"];
export type FeedbackCreateRequest = components["schemas"]["FeedbackCreateRequest"];
export type FeedbackResponse = components["schemas"]["FeedbackResponse"];
export type FeedbackWriterResponse = components["schemas"]["FeedbackWriterResponse"];
export type FeedbackPage = components["schemas"]["PageResponseFeedbackResponse"];
export type FeedbackStatus = NonNullable<FeedbackResponse["status"]>;
export type FeedbackFilter = "ALL" | FeedbackStatus;
