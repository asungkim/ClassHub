"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { lessonComposerQueryKeys } from "@/hooks/api/lesson-composer-keys";
import type { LessonComposerStudent } from "@/types/api/lesson";

export function useCourseStudents(courseId?: string) {
  return useQuery({
    queryKey: lessonComposerQueryKeys.courseStudents(courseId),
    enabled: Boolean(courseId),
    queryFn: async () => {
      if (!courseId) {
        return [] as LessonComposerStudent[];
      }

      const response = await api.GET("/api/v1/courses/{courseId}/students", {
        params: { path: { courseId } }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "학생 목록을 불러오지 못했습니다."));
      }

      return (response.data?.data ?? []) as LessonComposerStudent[];
    },
    staleTime: 60 * 1000
  });
}
