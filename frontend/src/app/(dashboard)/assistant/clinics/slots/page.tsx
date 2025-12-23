"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicSlots } from "@/hooks/clinic/use-clinic-slots";
import { fetchAssistantCourses } from "@/lib/dashboard-api";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { TimeSelect } from "@/components/ui/time-select";
import { EmptyState } from "@/components/shared/empty-state";
import { WeeklyTimeGrid } from "@/components/shared/weekly-time-grid";
import type { components } from "@/types/openapi";

const DAY_OPTIONS = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" }
] as const;

const TIME_PLACEHOLDER = "--:--";
const GRID_START_HOUR = 6;
const GRID_END_HOUR = 22;
const GRID_HOUR_HEIGHT = 56;

const GRID_DAYS = DAY_OPTIONS.map((day) => ({ key: day.value, label: day.label }));

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];
type ClinicSlotCreateRequest = components["schemas"]["ClinicSlotCreateRequest"];

type SlotFormState = {
  dayOfWeek: ClinicSlotCreateRequest["dayOfWeek"];
  startTime: string;
  endTime: string;
  defaultCapacity: string;
};

const DEFAULT_FORM_STATE: SlotFormState = {
  dayOfWeek: "MONDAY",
  startTime: "09:00",
  endTime: "10:00",
  defaultCapacity: "6"
};

type AssistantContextOption = {
  key: string;
  teacherId: string;
  teacherName: string;
  branchId: string;
  branchName: string;
  companyName: string;
};

export default function AssistantClinicSlotsPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  const { showToast } = useToast();
  const [contexts, setContexts] = useState<AssistantContextOption[]>([]);
  const [contextLoading, setContextLoading] = useState(false);
  const [contextError, setContextError] = useState<string | null>(null);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formState, setFormState] = useState<SlotFormState>(DEFAULT_FORM_STATE);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadContexts = useCallback(async () => {
    setContextLoading(true);
    setContextError(null);
    try {
      const result = await fetchAssistantCourses({ status: "ACTIVE", page: 0, size: 200 });
      const map = new Map<string, AssistantContextOption>();
      result.items.forEach((course) => {
        if (!course.teacherId || !course.branchId) {
          return;
        }
        const key = `${course.teacherId}:${course.branchId}`;
        if (!map.has(key)) {
          map.set(key, {
            key,
            teacherId: course.teacherId,
            teacherName: course.teacherName ?? "선생님",
            branchId: course.branchId,
            branchName: course.branchName ?? "지점",
            companyName: course.companyName ?? "학원"
          });
        }
      });
      setContexts(Array.from(map.values()));
    } catch (err) {
      const message = err instanceof Error ? err.message : "선생님/지점 목록을 불러오지 못했습니다.";
      setContextError(message);
    } finally {
      setContextLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadContexts();
  }, [loadContexts]);

  useEffect(() => {
    if (contexts.length === 0) {
      setSelectedKey(null);
      return;
    }
    if (selectedKey && contexts.some((item) => item.key === selectedKey)) {
      return;
    }
    setSelectedKey(contexts[0]?.key ?? null);
  }, [contexts, selectedKey]);

  const selectedContext = useMemo(
    () => contexts.find((context) => context.key === selectedKey) ?? null,
    [contexts, selectedKey]
  );

  const {
    slots,
    isLoading: slotsLoading,
    error: slotsError,
    refresh: refreshSlots
  } = useClinicSlots(
    {
      branchId: selectedContext?.branchId,
      teacherId: selectedContext?.teacherId
    },
    Boolean(selectedContext)
  );

  const slotsByDay = useMemo(() => {
    const base: Record<string, ClinicSlotResponse[]> = {};
    DAY_OPTIONS.forEach((day) => {
      base[day.value] = [];
    });
    slots.forEach((slot) => {
      if (!slot.dayOfWeek) {
        return;
      }
      base[slot.dayOfWeek] = [...(base[slot.dayOfWeek] ?? []), slot];
    });
    Object.values(base).forEach((list) => {
      list.sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
    });
    return base;
  }, [slots]);

  const openCreateModal = (preset?: Partial<SlotFormState>) => {
    setFormState({ ...DEFAULT_FORM_STATE, ...preset });
    setFormError(null);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    if (isSubmitting) {
      return;
    }
    setIsModalOpen(false);
  };

  const handleSelectRange = (range: { dayKey: string; startTime: string; endTime: string }) => {
    if (!selectedContext) {
      showToast("error", "선생님/지점을 먼저 선택해 주세요.");
      return;
    }
    openCreateModal({
      dayOfWeek: range.dayKey as SlotFormState["dayOfWeek"],
      startTime: range.startTime,
      endTime: range.endTime
    });
  };

  const submitSlot = async () => {
    if (!selectedContext) {
      showToast("error", "선생님/지점을 먼저 선택해 주세요.");
      return;
    }
    const capacity = Number(formState.defaultCapacity);
    if (!formState.dayOfWeek || !formState.startTime || !formState.endTime) {
      setFormError("요일과 시간을 모두 입력해주세요.");
      return;
    }
    if (!Number.isFinite(capacity) || capacity < 1) {
      setFormError("정원은 1명 이상이어야 합니다.");
      return;
    }
    if (formState.startTime >= formState.endTime) {
      setFormError("종료 시간이 시작 시간보다 늦어야 합니다.");
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      const body: ClinicSlotCreateRequest = {
        branchId: selectedContext.branchId,
        dayOfWeek: formState.dayOfWeek,
        startTime: formState.startTime,
        endTime: formState.endTime,
        defaultCapacity: capacity
      };
      const response = await api.POST("/api/v1/clinic-slots", { body });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯을 생성하지 못했습니다."));
      }
      if (!response.data?.data) {
        throw new Error("슬롯을 생성하지 못했습니다.");
      }
      showToast("success", "슬롯이 생성되었습니다.");
      setIsModalOpen(false);
      await refreshSlots();
    } catch (err) {
      const message = err instanceof Error ? err.message : "슬롯 저장에 실패했습니다.";
      setFormError(message);
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="선생님별 클리닉 (슬롯)" description="담당 선생님 기준 슬롯을 조회합니다.">
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">선생님/지점 선택</p>
            <p className="text-xs text-slate-500">담당 선생님과 지점을 선택하면 슬롯을 확인할 수 있습니다.</p>
          </div>

          {contextError && (
            <div className="space-y-3">
              <InlineError message={contextError} />
              <Button variant="secondary" onClick={() => void loadContexts()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {contextLoading && <Skeleton className="h-12 w-full max-w-sm" />}

          {!contextLoading && contexts.length === 0 && (
            <EmptyState message="배정된 반이 없습니다." description="선생님 배정 후 슬롯을 확인할 수 있습니다." />
          )}

          {!contextLoading && contexts.length > 0 && (
            <Select
              label="선생님/지점"
              value={selectedKey ?? ""}
              onChange={(event) => setSelectedKey(event.target.value)}
              className="md:w-96"
            >
              {contexts.map((context) => (
                <option key={context.key} value={context.key}>
                  {context.teacherName} · {context.branchName} ({context.companyName})
                </option>
              ))}
            </Select>
          )}
        </div>
      </Card>

      <Card
        title="슬롯 시간표"
        description={selectedContext ? `${selectedContext.teacherName} 슬롯 시간표입니다.` : ""}
      >
        <div className="space-y-6">
          {slotsError && (
            <div className="space-y-3">
              <InlineError message={slotsError} />
              <Button variant="secondary" onClick={() => void refreshSlots()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {slotsLoading && (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-[640px] w-full" />
            </div>
          )}

          {!slotsLoading && !selectedContext && (
            <EmptyState message="선생님/지점을 먼저 선택해 주세요." description="선택 후 슬롯이 표시됩니다." />
          )}

          {!slotsLoading && selectedContext && !slotsError && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm text-slate-500">
                  셀을 드래그/롱프레스하면 슬롯 생성이 시작됩니다.
                </p>
                {slots.length === 0 && (
                  <p className="text-sm font-semibold text-slate-700">등록된 슬롯이 없습니다.</p>
                )}
              </div>
              <WeeklyTimeGrid
                days={GRID_DAYS}
                itemsByDay={slotsByDay}
                startHour={GRID_START_HOUR}
                endHour={GRID_END_HOUR}
                hourHeight={GRID_HOUR_HEIGHT}
                showDateHeader={false}
                selectionEnabled={Boolean(selectedContext)}
                onSelectRange={handleSelectRange}
                getItemRange={(slot) => ({
                  startTime: slot.startTime,
                  endTime: slot.endTime
                })}
                getItemKey={(slot, index) => slot.slotId ?? `${slot.dayOfWeek}-${slot.startTime}-${index}`}
                renderItem={({ item, style }) => (
                  <div
                    className="absolute left-1 right-1 rounded-2xl border border-slate-200 bg-white p-2 text-xs text-slate-700 shadow-sm"
                    style={style}
                  >
                    <p className="truncate text-sm font-semibold text-slate-900">
                      {formatTime(item.startTime)} - {formatTime(item.endTime)}
                    </p>
                    <p className="text-[11px] text-slate-500">정원 {item.defaultCapacity ?? "-"}</p>
                  </div>
                )}
              />
            </div>
          )}
        </div>
      </Card>

      <Modal open={isModalOpen} onClose={closeModal} title="슬롯 추가" size="sm">
        <div className="space-y-4">
          <Select
            label="요일"
            value={formState.dayOfWeek}
            onChange={(event) => setFormState((prev) => ({ ...prev, dayOfWeek: event.target.value as SlotFormState["dayOfWeek"] }))}
          >
            {DAY_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>

          <TimeSelect
            label="시작 시간"
            value={formState.startTime}
            onChange={(value) => setFormState((prev) => ({ ...prev, startTime: value }))}
          />

          <TimeSelect
            label="종료 시간"
            value={formState.endTime}
            onChange={(value) => setFormState((prev) => ({ ...prev, endTime: value }))}
          />

          <TextField
            label="기본 정원"
            type="number"
            min={1}
            value={formState.defaultCapacity}
            onChange={(event) => setFormState((prev) => ({ ...prev, defaultCapacity: event.target.value }))}
          />

          {formError && <InlineError message={formError} />}

          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={closeModal} disabled={isSubmitting}>
              취소
            </Button>
            <Button onClick={() => void submitSlot()} disabled={isSubmitting}>
              {isSubmitting ? "저장 중..." : "저장"}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
