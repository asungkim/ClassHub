import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { components } from "@/types/openapi";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { toast } from "sonner";

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];
type ClinicSlotCreateRequest = components["schemas"]["ClinicSlotCreateRequest"];
type ClinicSlotUpdateRequest = components["schemas"]["ClinicSlotUpdateRequest"];
// GET /api/v1/clinic-slots
export function useClinicSlots() {
  return useQuery<ClinicSlotResponse[]>({
    queryKey: ["clinic-slots"],
    queryFn: async () => {
      const response = await api.GET("/api/v1/clinic-slots", {});
      const fetchError = getFetchError(response);
      if (fetchError) {
        if (fetchError.status === 404) {
          return [];
        }
        throw new Error(getApiErrorMessage(fetchError, "클리닉 슬롯 조회에 실패했습니다."));
      }
      return response.data?.data ?? [];
    }
  });
}

// POST /api/v1/clinic-slots
export function useCreateClinicSlot() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: ClinicSlotCreateRequest) => {
      const response = await api.POST("/api/v1/clinic-slots", { body: request });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯 생성에 실패했습니다."));
      }
      return response.data?.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clinic-slots"] });
      toast.success("클리닉 슬롯이 생성되었습니다");
    },
    onError: (error) => {
      const err = error as Error;
      toast.error(err.message || "슬롯 생성에 실패했습니다");
    }
  });
}

// PATCH /api/v1/clinic-slots/{slotId}
export function useUpdateClinicSlot() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ slotId, request }: { slotId: string; request: ClinicSlotUpdateRequest }) => {
      const response = await api.PATCH("/api/v1/clinic-slots/{slotId}", {
        params: { path: { slotId } },
        body: request
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯 수정에 실패했습니다."));
      }
      return response.data?.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clinic-slots"] });
      toast.success("클리닉 슬롯이 수정되었습니다");
    },
    onError: (error) => {
      const err = error as Error;
      toast.error(err.message || "슬롯 수정에 실패했습니다");
    }
  });
}

// DELETE /api/v1/clinic-slots/{slotId}
export function useDeleteClinicSlot() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (slotId: string) => {
      const response = await api.DELETE("/api/v1/clinic-slots/{slotId}", { params: { path: { slotId } } });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯 삭제에 실패했습니다."));
      }
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clinic-slots"] });
      toast.success("클리닉 슬롯이 삭제되었습니다");
    },
    onError: (error) => {
      const err = error as Error;
      toast.error(err.message || "슬롯 삭제에 실패했습니다");
    }
  });
}

// PATCH /api/v1/clinic-slots/{slotId}/deactivate
export function useDeactivateClinicSlot() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (slotId: string) => {
      const response = await api.PATCH("/api/v1/clinic-slots/{slotId}/deactivate", {
        params: { path: { slotId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯 비활성화에 실패했습니다."));
      }
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clinic-slots"] });
      toast.success("클리닉 슬롯이 비활성화되었습니다");
    },
    onError: (error) => {
      const err = error as Error;
      toast.error(err.message || "슬롯 비활성화에 실패했습니다");
    }
  });
}

// PATCH /api/v1/clinic-slots/{slotId}/activate
export function useActivateClinicSlot() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (slotId: string) => {
      const response = await api.PATCH("/api/v1/clinic-slots/{slotId}/activate", {
        params: { path: { slotId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯 활성화에 실패했습니다."));
      }
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clinic-slots"] });
      toast.success("클리닉 슬롯이 활성화되었습니다");
    },
    onError: (error) => {
      const err = error as Error;
      toast.error(err.message || "슬롯 활성화에 실패했습니다");
    }
  });
}

// Helper: Check for time conflicts
export function useSlotConflictCheck(excludeId?: string) {
  const { data: slots = [] } = useClinicSlots();

  return (dayOfWeek: string, startTime: string, endTime: string) => {
    return getClinicSlotConflict(slots, dayOfWeek, startTime, endTime, excludeId);
  };
}

function timeToMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

export function getClinicSlotConflict(
  slots: ClinicSlotResponse[],
  dayOfWeek: string,
  startTime: string,
  endTime: string,
  excludeId?: string
) {
  const start = timeToMinutes(startTime);
  const end = timeToMinutes(endTime);

  return slots.find((slot) => {
    if (!slot.isActive) return false;
    if (slot.id === excludeId) return false;
    if (slot.dayOfWeek !== dayOfWeek) return false;

    const slotStart = timeToMinutes((slot.startTime ?? "00:00").slice(0, 5));
    const slotEnd = timeToMinutes((slot.endTime ?? "00:00").slice(0, 5));

    return start < slotEnd && slotStart < end;
  });
}
