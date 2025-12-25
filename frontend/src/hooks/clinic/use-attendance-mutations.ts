"use client";

import { useCallback, useState } from "react";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type StudentClinicAttendanceRequest = components["schemas"]["StudentClinicAttendanceRequest"];
type ClinicAttendanceMoveRequest = components["schemas"]["ClinicAttendanceMoveRequest"];
type ClinicAttendanceResponse = components["schemas"]["ClinicAttendanceResponse"];

type AttendanceMutationState = {
  requestAttendance: (payload: StudentClinicAttendanceRequest) => Promise<ClinicAttendanceResponse>;
  moveAttendance: (payload: ClinicAttendanceMoveRequest) => Promise<ClinicAttendanceResponse>;
  isRequesting: boolean;
  isMoving: boolean;
  error: string | null;
  clearError: () => void;
};

const REQUEST_FALLBACK = "클리닉 참석 신청에 실패했습니다.";
const MOVE_FALLBACK = "클리닉 이동에 실패했습니다.";

export function useAttendanceMutations(): AttendanceMutationState {
  const [isRequesting, setIsRequesting] = useState(false);
  const [isMoving, setIsMoving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

  const requestAttendance = useCallback(async (payload: StudentClinicAttendanceRequest) => {
    setIsRequesting(true);
    setError(null);
    try {
      const response = await api.POST("/api/v1/students/me/clinic-attendances" as const, {
        body: payload
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, REQUEST_FALLBACK));
      }
      const attendance = response.data?.data;
      if (!attendance) {
        throw new Error(REQUEST_FALLBACK);
      }
      return attendance as ClinicAttendanceResponse;
    } catch (err) {
      const message = err instanceof Error ? err.message : REQUEST_FALLBACK;
      setError(message);
      throw err;
    } finally {
      setIsRequesting(false);
    }
  }, []);

  const moveAttendance = useCallback(async (payload: ClinicAttendanceMoveRequest) => {
    setIsMoving(true);
    setError(null);
    try {
      const response = await api.PATCH("/api/v1/students/me/clinic-attendances" as const, {
        body: payload
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, MOVE_FALLBACK));
      }
      const attendance = response.data?.data;
      if (!attendance) {
        throw new Error(MOVE_FALLBACK);
      }
      return attendance as ClinicAttendanceResponse;
    } catch (err) {
      const message = err instanceof Error ? err.message : MOVE_FALLBACK;
      setError(message);
      throw err;
    } finally {
      setIsMoving(false);
    }
  }, []);

  return { requestAttendance, moveAttendance, isRequesting, isMoving, error, clearError };
}
