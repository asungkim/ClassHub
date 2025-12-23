"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];
type ClinicSlotListResponse = components["schemas"]["RsDataListClinicSlotResponse"];
type ClinicSlotData = Required<ClinicSlotListResponse>["data"];

type ClinicSlotQuery = {
  branchId?: string;
  teacherId?: string;
  courseId?: string;
};

type ClinicSlotState = {
  slots: ClinicSlotResponse[];
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
};

const FALLBACK_MESSAGE = "클리닉 슬롯을 가져오지 못했습니다.";

export function useClinicSlots(query: ClinicSlotQuery, enabled = true): ClinicSlotState {
  const [slots, setSlots] = useState<ClinicSlotResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { branchId, teacherId, courseId } = query;

  const fetchSlots = useCallback(async () => {
    if (!enabled || (!branchId && !courseId)) {
      setSlots([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.GET("/api/v1/clinic-slots" as const, {
        params: {
          query: {
            branchId,
            teacherId,
            courseId
          }
        }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, FALLBACK_MESSAGE));
      }
      const data = (response.data?.data ?? []) as ClinicSlotData;
      setSlots(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : FALLBACK_MESSAGE);
      setSlots([]);
    } finally {
      setIsLoading(false);
    }
  }, [branchId, courseId, enabled, teacherId]);

  useEffect(() => {
    void fetchSlots();
  }, [fetchSlots]);

  return { slots, isLoading, error, refresh: fetchSlots };
}
