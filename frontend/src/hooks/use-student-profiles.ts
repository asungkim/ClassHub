"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type PageResponseStudentProfileSummary = components["schemas"]["PageResponseStudentProfileSummary"];
type Pageable = components["schemas"]["Pageable"];
type StudentProfileCreateRequest = components["schemas"]["StudentProfileCreateRequest"];
type StudentProfileUpdateRequest = components["schemas"]["StudentProfileUpdateRequest"];
type StudentProfileResponse = components["schemas"]["StudentProfileResponse"];

type StudentFilters = {
  active?: boolean;
  name?: string;
  page: number;
};

const PAGE_SIZE = 20;
const DEFAULT_SORT = ["createdAt,desc"];

function buildPageable(page: number): Pageable {
  return {
    page,
    size: PAGE_SIZE,
    sort: DEFAULT_SORT
  };
}

function serializeStudentQuery(query: { name?: string; active?: boolean; pageable: Pageable }) {
  const search = new URLSearchParams();

  if (query.name) {
    search.set("name", query.name);
  }
  if (typeof query.active === "boolean") {
    search.set("active", String(query.active));
  }

  const { page = 0, size = PAGE_SIZE, sort = DEFAULT_SORT } = query.pageable;
  search.set("page", String(page));
  search.set("size", String(size));
  sort?.forEach((value) => {
    if (value) {
      search.append("sort", value);
    }
  });

  return search.toString();
}

function mapPageResponse(
  data: PageResponseStudentProfileSummary | null | undefined,
  fallbackPage: number
): PageResponseStudentProfileSummary {
  return (
    data ?? {
      content: [],
      page: fallbackPage,
      size: PAGE_SIZE,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    }
  );
}

export function useStudentProfileList(filters: StudentFilters) {
  const { active, name, page } = filters;
  const normalizedFilters = { active, name: name ?? "", page };

  return useQuery({
    queryKey: ["student-profiles", normalizedFilters],
    queryFn: async () => {
      const response = await api.GET("/api/v1/student-profiles", {
        params: {
          query: {
            name: name || undefined,
            active: typeof active === "boolean" ? active : undefined,
            pageable: buildPageable(page)
          }
        },
        querySerializer: serializeStudentQuery
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return mapPageResponse(response.data?.data, page);
    },
    placeholderData: keepPreviousData
  });
}

export function useCreateStudentProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (body: StudentProfileCreateRequest) => {
      const response = await api.POST("/api/v1/student-profiles", { body });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }
      return response.data?.data;
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["student-profiles"] });
      const studentName = variables.name ?? "학생";
      toast.success(`${studentName} 학생을 등록했습니다.`);
    },
    onError: (error, variables) => {
      const studentName = variables.name ?? "학생";
      toast.error(getApiErrorMessage(error, `${studentName} 학생 등록에 실패했습니다.`));
    }
  });
}

export function useStudentProfileDetail(profileId: string) {
  return useQuery({
    queryKey: ["student-profile", profileId],
    queryFn: async () => {
      const response = await api.GET("/api/v1/student-profiles/{profileId}", {
        params: { path: { profileId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }
      return response.data?.data as StudentProfileResponse | undefined;
    },
    enabled: Boolean(profileId)
  });
}

export function useUpdateStudentProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ profileId, body }: { profileId: string; body: StudentProfileUpdateRequest }) => {
      const response = await api.PATCH("/api/v1/student-profiles/{profileId}", {
        params: { path: { profileId } },
        body
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }
      return response.data?.data;
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["student-profiles"] });
      void queryClient.invalidateQueries({ queryKey: ["student-profile", variables.profileId] });
      toast.success("학생 정보를 수정했습니다.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "학생 정보 수정에 실패했습니다."));
    }
  });
}
