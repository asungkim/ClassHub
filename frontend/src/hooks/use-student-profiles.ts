"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type PageResponseStudentProfileSummary = components["schemas"]["PageResponseStudentProfileSummary"];
type Pageable = components["schemas"]["Pageable"];

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

function serializeStudentQuery(query: {
  name?: string;
  active?: boolean;
  pageable: Pageable;
}) {
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
