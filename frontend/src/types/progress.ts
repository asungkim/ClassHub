import type { components } from "@/types/openapi";

export type CourseProgressResponse = components["schemas"]["CourseProgressResponse"];
export type PersonalProgressResponse = components["schemas"]["PersonalProgressResponse"];
export type CourseProgressCreateRequest = components["schemas"]["CourseProgressCreateRequest"];
export type CourseProgressUpdateRequest = components["schemas"]["CourseProgressUpdateRequest"];
export type CourseProgressComposeRequest = components["schemas"]["CourseProgressComposeRequest"];
export type PersonalProgressComposeRequest = components["schemas"]["PersonalProgressComposeRequest"];
export type PersonalProgressCreateRequest = components["schemas"]["PersonalProgressCreateRequest"];
export type PersonalProgressUpdateRequest = components["schemas"]["PersonalProgressUpdateRequest"];
export type ProgressCursor = components["schemas"]["ProgressCursor"];
export type CourseProgressSlice = components["schemas"]["ProgressSliceResponseCourseProgressResponse"];
export type PersonalProgressSlice = components["schemas"]["ProgressSliceResponsePersonalProgressResponse"];
export type StudentCalendarResponse = components["schemas"]["StudentCalendarResponse"];
export type CourseProgressEvent = components["schemas"]["CourseProgressEvent"] & { content?: string };
export type PersonalProgressEvent = components["schemas"]["PersonalProgressEvent"] & { content?: string };
export type ClinicRecordSummary = components["schemas"]["ClinicRecordSummary"] & { content?: string };
export type ClinicEvent = Omit<components["schemas"]["ClinicEvent"], "recordSummary"> & {
  recordSummary?: ClinicRecordSummary;
};
