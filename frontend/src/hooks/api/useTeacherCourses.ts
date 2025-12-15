"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { lessonComposerQueryKeys } from "@/hooks/api/lesson-composer-keys";
import type { LessonComposerCourse } from "@/types/api/lesson";

export function useTeacherCourses(isActive = true) {
  return useQuery({
    queryKey: lessonComposerQueryKeys.courses(isActive),
    queryFn: async () => {
      const response = await api.GET("/api/v1/courses", {
        params: {
          query: { isActive }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "반 목록을 불러오지 못했습니다."));
      }

      return (response.data?.data ?? []) as LessonComposerCourse[];
    },
    staleTime: 60 * 1000
  });
}
