"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type StudentClinicAttendanceResponse = components["schemas"]["StudentClinicAttendanceResponse"];
type StudentClinicAttendanceListResponse = components["schemas"]["StudentClinicAttendanceListResponse"];
type StudentAttendancesRsData = StudentClinicAttendanceResponse[];

type StudentAttendancesState = {
  attendances: StudentClinicAttendanceResponse[];
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
};

const FALLBACK_MESSAGE = "클리닉 참석 정보를 가져오지 못했습니다.";

export function useStudentAttendances(dateRange?: string, enabled = true): StudentAttendancesState {
  const [attendances, setAttendances] = useState<StudentClinicAttendanceResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAttendances = useCallback(async () => {
    if (!enabled || !dateRange) {
      setAttendances([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const queryParams = { dateRange };
      const response = await api.GET("/api/v1/students/me/clinic-attendances" as const, {
        params: { query: queryParams }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, FALLBACK_MESSAGE));
      }
      const data = (response.data?.data?.items ?? []) as StudentAttendancesRsData;
      setAttendances(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : FALLBACK_MESSAGE);
      setAttendances([]);
    } finally {
      setIsLoading(false);
    }
  }, [dateRange, enabled]);

  useEffect(() => {
    void fetchAttendances();
  }, [fetchAttendances]);

  return { attendances, isLoading, error, refresh: fetchAttendances };
}
