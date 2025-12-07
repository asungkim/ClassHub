"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";
type PageResponseMemberSummary = components["schemas"]["PageResponseMemberSummary"];
type Pageable = components["schemas"]["Pageable"];

type AssistantFilters = {
  active?: boolean;
  name?: string;
  page: number;
};

type ToggleAssistantInput = {
  memberId: string;
  name: string;
  active: boolean;
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

function serializeAssistantQuery(query: {
  role: string;
  name?: string;
  active?: boolean;
  pageable: Pageable;
}) {
  const search = new URLSearchParams();
  search.set("role", query.role);

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
  data: PageResponseMemberSummary | null | undefined,
  fallbackPage: number
): PageResponseMemberSummary {
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

export function useAssistantList(filters: AssistantFilters) {
  const { active, name, page } = filters;
  const normalizedFilters = {
    active,
    name: name ?? "",
    page
  };

  return useQuery({
    queryKey: ["assistants", normalizedFilters],
    queryFn: async () => {
      const response = await api.GET("/api/v1/members", {
        params: {
          query: {
            role: "ASSISTANT",
            name: name || undefined,
            active: typeof active === "boolean" ? active : undefined,
            pageable: buildPageable(page)
          }
        },
        querySerializer: serializeAssistantQuery
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

export function useToggleAssistantActive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ memberId, active }: ToggleAssistantInput) => {
      const path = active ? "/api/v1/members/{memberId}/deactivate" : "/api/v1/members/{memberId}/activate";
      const response = await api.PATCH(path, {
        params: { path: { memberId } }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data;
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["assistants"] });
      const message = variables.active
          ? `${variables.name} 조교를 비활성화했습니다.`
          : `${variables.name} 조교를 활성화했습니다.`;
      toast.success(message);
    },
    onError: (error, variables) => {
      const fallback = variables.active
          ? `${variables.name} 조교 비활성화에 실패했습니다.`
          : `${variables.name} 조교 활성화에 실패했습니다.`;
      toast.error(getApiErrorMessage(error, fallback));
    }
  });
}
