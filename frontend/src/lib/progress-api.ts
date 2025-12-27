import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import type {
  CourseProgressComposeRequest,
  CourseProgressCreateRequest,
  CourseProgressResponse,
  CourseProgressSlice,
  CourseProgressUpdateRequest,
  ClinicRecordResponse,
  ClinicRecordUpdateRequest,
  PersonalProgressCreateRequest,
  PersonalProgressResponse,
  PersonalProgressSlice,
  PersonalProgressUpdateRequest,
  ProgressCursor,
  StudentCalendarResponse
} from "@/types/progress";

export const PROGRESS_PAGE_LIMIT = 20;

type ProgressSliceResult<T> = {
  items: T[];
  nextCursor?: ProgressCursor;
};

export async function fetchCourseProgresses(params: {
  courseId: string;
  cursor?: ProgressCursor | null;
  limit?: number;
}): Promise<ProgressSliceResult<CourseProgressResponse>> {
  const { courseId, cursor, limit = PROGRESS_PAGE_LIMIT } = params;
  const response = await api.GET("/api/v1/courses/{courseId}/course-progress", {
    params: {
      path: { courseId },
      query: {
        cursorCreatedAt: cursor?.createdAt,
        cursorId: cursor?.id,
        limit
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "반 공통 진도를 불러오지 못했습니다."));
  }

  const payload = response.data.data as CourseProgressSlice;
  return {
    items: payload.items ?? [],
    nextCursor: payload.nextCursor
  };
}

export async function createCourseProgress(courseId: string, payload: CourseProgressCreateRequest) {
  const response = await api.POST("/api/v1/courses/{courseId}/course-progress", {
    params: { path: { courseId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "공통 진도를 저장하지 못했습니다."));
  }

  return response.data.data as CourseProgressResponse;
}

export async function composeCourseProgress(courseId: string, payload: CourseProgressComposeRequest) {
  const response = await api.POST("/api/v1/courses/{courseId}/course-progress/compose", {
    params: { path: { courseId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "통합 수업 기록을 저장하지 못했습니다."));
  }

  return response.data.data as CourseProgressResponse;
}

export async function updateCourseProgress(progressId: string, payload: CourseProgressUpdateRequest) {
  const response = await api.PATCH("/api/v1/course-progress/{progressId}", {
    params: { path: { progressId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "공통 진도를 수정하지 못했습니다."));
  }

  return response.data.data as CourseProgressResponse;
}

export async function deleteCourseProgress(progressId: string) {
  const response = (await api.DELETE("/api/v1/course-progress/{progressId}", {
    params: { path: { progressId } }
  })) as { error?: unknown };

  if (response.error) {
    throw new Error(getApiErrorMessage(response.error, "공통 진도를 삭제하지 못했습니다."));
  }
}

export async function fetchPersonalProgresses(params: {
  recordId: string;
  cursor?: ProgressCursor | null;
  limit?: number;
}): Promise<ProgressSliceResult<PersonalProgressResponse>> {
  const { recordId, cursor, limit = PROGRESS_PAGE_LIMIT } = params;
  const response = await api.GET("/api/v1/student-courses/{recordId}/personal-progress", {
    params: {
      path: { recordId },
      query: {
        cursorCreatedAt: cursor?.createdAt,
        cursorId: cursor?.id,
        limit
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "개인 진도를 불러오지 못했습니다."));
  }

  const payload = response.data.data as PersonalProgressSlice;
  return {
    items: payload.items ?? [],
    nextCursor: payload.nextCursor
  };
}

export async function createPersonalProgress(recordId: string, payload: PersonalProgressCreateRequest) {
  const response = await api.POST("/api/v1/student-courses/{recordId}/personal-progress", {
    params: { path: { recordId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "개인 진도를 저장하지 못했습니다."));
  }

  return response.data.data as PersonalProgressResponse;
}

export async function updatePersonalProgress(progressId: string, payload: PersonalProgressUpdateRequest) {
  const response = await api.PATCH("/api/v1/personal-progress/{progressId}", {
    params: { path: { progressId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "개인 진도를 수정하지 못했습니다."));
  }

  return response.data.data as PersonalProgressResponse;
}

export async function deletePersonalProgress(progressId: string) {
  const response = (await api.DELETE("/api/v1/personal-progress/{progressId}", {
    params: { path: { progressId } }
  })) as { error?: unknown };

  if (response.error) {
    throw new Error(getApiErrorMessage(response.error, "개인 진도를 삭제하지 못했습니다."));
  }
}

export async function updateClinicRecord(recordId: string, payload: ClinicRecordUpdateRequest) {
  const response = await api.PATCH("/api/v1/clinic-records/{recordId}", {
    params: { path: { recordId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "클리닉 기록을 수정하지 못했습니다."));
  }

  return response.data.data as ClinicRecordResponse;
}

export async function deleteClinicRecord(recordId: string) {
  const response = (await api.DELETE("/api/v1/clinic-records/{recordId}", {
    params: { path: { recordId } }
  })) as { error?: unknown };

  if (response.error) {
    throw new Error(getApiErrorMessage(response.error, "클리닉 기록을 삭제하지 못했습니다."));
  }
}

export async function fetchStudentCalendar(params: {
  studentId: string;
  year: number;
  month: number;
}): Promise<StudentCalendarResponse> {
  const { studentId, year, month } = params;
  const response = await api.GET("/api/v1/students/{studentId}/calendar", {
    params: {
      path: { studentId },
      query: { year, month }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생 캘린더를 불러오지 못했습니다."));
  }

  return response.data.data as StudentCalendarResponse;
}
