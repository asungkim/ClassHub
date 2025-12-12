import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type CourseResponse = components["schemas"]["CourseResponse"];
type CourseCreateRequest = components["schemas"]["CourseCreateRequest"];
type CourseUpdateRequest = components["schemas"]["CourseUpdateRequest"];

// Query Keys
const courseKeys = {
  all: ["courses"] as const,
  lists: () => [...courseKeys.all, "list"] as const,
  list: (isActive?: boolean) => [...courseKeys.lists(), { isActive }] as const,
  detail: (id: string) => [...courseKeys.all, "detail", id] as const
};

// ========== 목록 조회 ==========
export function useCourses(isActive?: boolean) {
  return useQuery({
    queryKey: courseKeys.list(isActive),
    queryFn: async () => {
      const response = await api.GET("/api/v1/courses", {
        params: {
          query: {
            isActive: isActive !== undefined ? isActive : undefined
          }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "반 목록을 불러오지 못했습니다."));
      }

      return (response.data?.data || []) as CourseResponse[];
    }
  });
}

// ========== 생성 ==========
export function useCreateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: CourseCreateRequest) => {
      const response = await api.POST("/api/v1/courses", { body: request });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "반 생성에 실패했습니다."));
      }

      return response.data?.data as CourseResponse;
    },
    onSuccess: () => {
      // 모든 목록 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() });
    }
  });
}

// ========== 수정 ==========
export function useUpdateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ courseId, request }: { courseId: string; request: CourseUpdateRequest }) => {
      const response = await api.PATCH("/api/v1/courses/{courseId}", {
        params: { path: { courseId } },
        body: request
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "반 수정에 실패했습니다."));
      }

      return response.data?.data as CourseResponse;
    },
    onSuccess: (_, variables) => {
      // 목록 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() });
      // 상세 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) });
    }
  });
}

// ========== 활성화 ==========
export function useActivateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (courseId: string) => {
      const response = await api.PATCH("/api/v1/courses/{courseId}/activate", {
        params: { path: { courseId } }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "반 활성화에 실패했습니다."));
      }
    },
    onSuccess: () => {
      // 모든 목록 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() });
    }
  });
}

// ========== 비활성화 ==========
export function useDeactivateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (courseId: string) => {
      const response = await api.PATCH("/api/v1/courses/{courseId}/deactivate", {
        params: { path: { courseId } }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "반 비활성화에 실패했습니다."));
      }
    },
    onSuccess: () => {
      // 모든 목록 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() });
    }
  });
}

// ========== 토글 헬퍼 (활성/비활성 자동 판단) ==========
export function useToggleCourse() {
  const activateMutation = useActivateCourse();
  const deactivateMutation = useDeactivateCourse();

  return {
    mutate: (courseId: string, isCurrentlyActive: boolean) => {
      if (isCurrentlyActive) {
        deactivateMutation.mutate(courseId);
      } else {
        activateMutation.mutate(courseId);
      }
    },
    isPending: activateMutation.isPending || deactivateMutation.isPending
  };
}
