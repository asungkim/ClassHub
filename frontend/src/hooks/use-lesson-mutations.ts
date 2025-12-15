"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { getFetchError } from "@/lib/api-error";

/**
 * SharedLesson 삭제 뮤테이션
 */
export function useDeleteSharedLesson() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (lessonId: string) => {
      const response = await api.DELETE("/api/v1/shared-lessons/{lessonId}", {
        params: {
          path: { lessonId }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data;
    },
    onSuccess: () => {
      // 학생 캘린더 캐시 무효화
      queryClient.invalidateQueries({ queryKey: ["student-calendar"] });
    }
  });
}

/**
 * PersonalLesson 삭제 뮤테이션
 */
export function useDeletePersonalLesson() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (lessonId: string) => {
      const response = await api.DELETE("/api/v1/personal-lessons/{lessonId}", {
        params: {
          path: { lessonId }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data;
    },
    onSuccess: () => {
      // 학생 캘린더 캐시 무효화
      queryClient.invalidateQueries({ queryKey: ["student-calendar"] });
    }
  });
}

/**
 * SharedLesson 수정 뮤테이션
 */
export function useUpdateSharedLesson() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: { title?: string; content?: string } }) => {
      const response = await api.PATCH("/api/v1/shared-lessons/{lessonId}", {
        params: {
          path: { lessonId: id }
        },
        body: data
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data;
    },
    onSuccess: () => {
      // 학생 캘린더 캐시 무효화
      queryClient.invalidateQueries({ queryKey: ["student-calendar"] });
    }
  });
}

/**
 * PersonalLesson 수정 뮤테이션
 */
export function useUpdatePersonalLesson() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: { content?: string } }) => {
      const response = await api.PATCH("/api/v1/personal-lessons/{lessonId}", {
        params: {
          path: { lessonId: id }
        },
        body: data
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data;
    },
    onSuccess: () => {
      // 학생 캘린더 캐시 무효화
      queryClient.invalidateQueries({ queryKey: ["student-calendar"] });
    }
  });
}
