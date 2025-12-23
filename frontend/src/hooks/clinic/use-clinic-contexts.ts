"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type ClinicContextResponse = components["schemas"]["StudentClinicContextResponse"];
type ClinicContextListResponse = components["schemas"]["RsDataListStudentClinicContextResponse"];
type ClinicContextData = Required<ClinicContextListResponse>["data"];

type ClinicContextState = {
  contexts: ClinicContextResponse[];
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
};

const FALLBACK_MESSAGE = "클리닉 컨텍스트를 가져오지 못했습니다.";

export function useClinicContexts(): ClinicContextState {
  const [contexts, setContexts] = useState<ClinicContextResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchContexts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.GET("/api/v1/students/me/clinic-contexts", {});
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, FALLBACK_MESSAGE));
      }
      const data = (response.data?.data ?? []) as ClinicContextData;
      setContexts(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : FALLBACK_MESSAGE);
      setContexts([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchContexts();
  }, [fetchContexts]);

  return { contexts, isLoading, error, refresh: fetchContexts };
}
