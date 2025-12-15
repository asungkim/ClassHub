"use client";

import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "@/hooks/use-debounce";
import { api } from "@/lib/api";
import { getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

// Type aliases from OpenAPI schema
type StudentCalendarResponse = components["schemas"]["StudentCalendarResponse"];
type PageResponseStudentProfileSummary = components["schemas"]["PageResponseStudentProfileSummary"];
type Pageable = components["schemas"]["Pageable"];

function serializeStudentQuery(query: { name?: string; pageable: Pageable }) {
  const search = new URLSearchParams();

  if (query.name) {
    search.set("name", query.name);
  }

  const { page = 0, size = 10, sort = ["name,asc"] } = query.pageable;
  search.set("page", String(page));
  search.set("size", String(size));
  sort?.forEach((value) => {
    if (value) {
      search.append("sort", value);
    }
  });

  return search.toString();
}

/**
 * 학생 캘린더 데이터를 조회하는 훅
 * @param studentId - 학생 프로필 ID
 * @param params - year, month 파라미터
 */
export function useStudentCalendar(
  studentId: string | undefined,
  params: { year: number; month: number }
) {
  const { year, month } = params;

  return useQuery({
    queryKey: ["student-calendar", studentId, year, month],
    queryFn: async () => {
      if (!studentId) {
        return null;
      }

      const response = await api.GET("/api/v1/students/{studentId}/calendar", {
        params: {
          path: { studentId },
          query: { year, month }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data?.data as StudentCalendarResponse | undefined;
    },
    enabled: Boolean(studentId) && year > 0 && month >= 1 && month <= 12
  });
}

/**
 * 학생 검색을 위한 훅 (디바운스 300ms 적용)
 * @param searchName - 검색할 학생 이름 (최소 1자 이상)
 */
export function useStudentProfiles(searchName: string) {
  const debouncedName = useDebounce(searchName, 300);
  const normalizedName = debouncedName.trim();
  const shouldFetch = normalizedName.length >= 1;

  return useQuery({
    queryKey: ["student-profiles-search", normalizedName],
    queryFn: async () => {
      const response = await api.GET("/api/v1/student-profiles", {
        params: {
          query: {
            name: normalizedName,
            pageable: {
              page: 0,
              size: 10,
              sort: ["name,asc"]
            }
          }
        },
        querySerializer: serializeStudentQuery
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      const pageData = response.data?.data as PageResponseStudentProfileSummary | undefined;
      return pageData?.content ?? [];
    },
    enabled: shouldFetch
  });
}
