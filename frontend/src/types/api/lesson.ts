import type { components, paths } from "@/types/openapi";

export type CreateSharedLessonOperation = paths["/api/v1/shared-lessons"]["post"];
export type CreatePersonalLessonOperation = paths["/api/v1/personal-lessons"]["post"];
export type GetTeacherCoursesOperation = paths["/api/v1/courses"]["get"];
export type GetCourseStudentsOperation = paths["/api/v1/courses/{courseId}/students"]["get"];

export type SharedLessonCreateRequest =
  CreateSharedLessonOperation["requestBody"] extends { content: { "application/json": infer Body } }
    ? Body
    : never;

export type PersonalLessonCreateRequest =
  CreatePersonalLessonOperation["requestBody"] extends { content: { "application/json": infer Body } }
    ? Body
    : never;

export type LessonComposerCourse = components["schemas"]["CourseResponse"];
export type LessonComposerStudent = components["schemas"]["StudentProfileSummary"];
export type SharedLessonResponse = components["schemas"]["SharedLessonResponse"];
export type PersonalLessonResponse = components["schemas"]["PersonalLessonResponse"];
