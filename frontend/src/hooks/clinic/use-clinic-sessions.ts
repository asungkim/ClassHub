"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type ClinicSessionResponse = components["schemas"]["ClinicSessionResponse"];
type ClinicSessionListResponse = components["schemas"]["RsDataListClinicSessionResponse"];
type ClinicSessionData = Required<ClinicSessionListResponse>["data"];

type ClinicSessionQuery = {
  dateRange?: string;
  branchId?: string;
  teacherId?: string;
};

type ClinicSessionState = {
  sessions: ClinicSessionResponse[];
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
};

const FALLBACK_MESSAGE = "클리닉 세션을 가져오지 못했습니다.";

export function useClinicSessions(query: ClinicSessionQuery, enabled = true): ClinicSessionState {
  const [sessions, setSessions] = useState<ClinicSessionResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSessions = useCallback(async () => {
    if (!enabled || !query.dateRange || !query.branchId) {
      setSessions([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const queryParams = {
        dateRange: query.dateRange,
        branchId: query.branchId,
        teacherId: query.teacherId
      };
      const response = await api.GET("/api/v1/clinic-sessions" as const, {
        params: { query: queryParams }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, FALLBACK_MESSAGE));
      }
      const data = (response.data?.data ?? []) as ClinicSessionData;
      setSessions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : FALLBACK_MESSAGE);
      setSessions([]);
    } finally {
      setIsLoading(false);
    }
  }, [enabled, query]);

  useEffect(() => {
    void fetchSessions();
  }, [fetchSessions]);

  return { sessions, isLoading, error, refresh: fetchSessions };
}
