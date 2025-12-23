"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicContexts } from "@/hooks/clinic/use-clinic-contexts";
import { useClinicSlots } from "@/hooks/clinic/use-clinic-slots";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { EmptyState } from "@/components/shared/empty-state";
import { WeeklyTimeGrid } from "@/components/shared/weekly-time-grid";
import type { components } from "@/types/openapi";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";

type ClinicContextCourse = {
  courseId: string;
  courseName: string;
  recordId?: string;
  defaultClinicSlotId?: string;
};

type ClinicContextGroup = {
  key: string;
  teacherId: string;
  branchId: string;
  teacherName: string;
  branchName: string;
  companyName: string;
  courses: ClinicContextCourse[];
};

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];
type StudentDefaultClinicSlotRequest = components["schemas"]["StudentDefaultClinicSlotRequest"];

const DAY_ORDER = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" }
];
const GRID_START_HOUR = 6;
const GRID_END_HOUR = 22;
const GRID_HOUR_HEIGHT = 56;
const GRID_DAYS = DAY_ORDER.map((day) => ({ key: day.value, label: day.label }));

const TIME_PLACEHOLDER = "--:--";

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);
const getDayLabel = (value?: ClinicSlotResponse["dayOfWeek"]) =>
  DAY_ORDER.find((day) => day.value === value)?.label ?? value ?? "";

export default function StudentClinicSchedulePage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  const { contexts, isLoading, error, refresh } = useClinicContexts();
  const { showToast } = useToast();
  const [selectedContextKey, setSelectedContextKey] = useState<string | null>(null);
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [pendingSlot, setPendingSlot] = useState<ClinicSlotResponse | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);

  const contextGroups = useMemo<ClinicContextGroup[]>(() => {
    const map = new Map<string, ClinicContextGroup>();

    contexts.forEach((context) => {
      const teacherId = context.teacherId ?? "";
      const branchId = context.branchId ?? "";
      const courseId = context.courseId ?? "";
      if (!teacherId || !branchId || !courseId) {
        return;
      }

      const key = `${teacherId}:${branchId}`;
      const existing = map.get(key);
      const course: ClinicContextCourse = {
        courseId,
        courseName: context.courseName ?? "반 이름 없음",
        recordId: context.recordId,
        defaultClinicSlotId: context.defaultClinicSlotId
      };

      if (existing) {
        const alreadyAdded = existing.courses.some((item) => item.courseId === courseId);
        if (!alreadyAdded) {
          existing.courses.push(course);
        }
      } else {
        map.set(key, {
          key,
          teacherId,
          branchId,
          teacherName: context.teacherName ?? "선생님",
          branchName: context.branchName ?? "지점",
          companyName: context.companyName ?? "학원",
          courses: [course]
        });
      }
    });

    return Array.from(map.values());
  }, [contexts]);

  useEffect(() => {
    if (contextGroups.length === 0) {
      if (selectedContextKey !== null) {
        setSelectedContextKey(null);
      }
      if (selectedCourseId !== null) {
        setSelectedCourseId(null);
      }
      return;
    }

    const nextContextKey =
      selectedContextKey && contextGroups.some((group) => group.key === selectedContextKey)
        ? selectedContextKey
        : contextGroups[0].key;
    const activeGroup = contextGroups.find((group) => group.key === nextContextKey);
    const nextCourseId =
      activeGroup && activeGroup.courses.some((course) => course.courseId === selectedCourseId)
        ? selectedCourseId
        : activeGroup?.courses[0]?.courseId ?? null;

    if (nextContextKey !== selectedContextKey) {
      setSelectedContextKey(nextContextKey);
    }
    if (nextCourseId !== selectedCourseId) {
      setSelectedCourseId(nextCourseId);
    }
  }, [contextGroups, selectedContextKey, selectedCourseId]);

  const handleSelectContext = useCallback((key: string) => {
    setSelectedContextKey(key);
  }, []);

  const handleRefresh = useCallback(() => {
    void refresh();
  }, [refresh]);

  const closeConfirm = useCallback(() => {
    setConfirmOpen(false);
    setPendingSlot(null);
  }, []);

  const selectedCourseContext = useMemo(
    () => contexts.find((context) => context.courseId === selectedCourseId) ?? null,
    [contexts, selectedCourseId]
  );

  const {
    slots,
    isLoading: slotsLoading,
    error: slotsError,
    refresh: refreshSlots
  } = useClinicSlots({ courseId: selectedCourseId ?? undefined }, Boolean(selectedCourseId));

  const slotsByDay = useMemo(() => {
    const base: Record<string, ClinicSlotResponse[]> = {};
    DAY_ORDER.forEach((day) => {
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

  const handleSlotSelect = useCallback(
    (slot: ClinicSlotResponse) => {
      if (!slot.slotId || !selectedCourseId || isUpdating) {
        return;
      }
      if (slot.slotId === selectedCourseContext?.defaultClinicSlotId) {
        showToast("info", "이미 기본 슬롯으로 설정되어 있습니다.");
        return;
      }
      setPendingSlot(slot);
      setConfirmOpen(true);
    },
    [isUpdating, selectedCourseContext?.defaultClinicSlotId, selectedCourseId, showToast]
  );

  const handleConfirmDefaultSlot = useCallback(async () => {
    if (!pendingSlot?.slotId || !selectedCourseId) {
      return;
    }
    setIsUpdating(true);
    try {
      const body: StudentDefaultClinicSlotRequest = { defaultClinicSlotId: pendingSlot.slotId };
      const response = await api.PATCH("/api/v1/students/me/courses/{courseId}/clinic-slot", {
        params: { path: { courseId: selectedCourseId } },
        body
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "기본 슬롯을 변경하지 못했습니다."));
      }
      if (!response.data?.data) {
        throw new Error("기본 슬롯을 변경하지 못했습니다.");
      }
      showToast("success", "기본 슬롯이 변경되었습니다.");
      closeConfirm();
      await refresh();
    } catch (err) {
      const message = err instanceof Error ? err.message : "기본 슬롯을 변경하지 못했습니다.";
      showToast("error", message);
    } finally {
      setIsUpdating(false);
    }
  }, [closeConfirm, pendingSlot?.slotId, refresh, selectedCourseId, showToast]);

  if (!canRender) {
    return fallback;
  }
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="클리닉 시간표" description="수업별 기본 슬롯을 선택하기 전에 선생님/지점 맥락을 먼저 고릅니다.">
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">선생님/지점 선택</p>
            <p className="text-xs text-slate-500">
              수강 중인 선생님과 지점을 선택하면 해당 반의 기본 슬롯을 설정할 수 있습니다.
            </p>
          </div>

          {error && (
            <div className="space-y-3">
              <InlineError message={error} />
              <Button variant="secondary" onClick={handleRefresh}>
                다시 불러오기
              </Button>
            </div>
          )}

          {isLoading && (
            <div className="grid gap-4 md:grid-cols-2">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-28 w-full" />
            </div>
          )}

          {!isLoading && contextGroups.length === 0 && (
            <EmptyState message="표시할 클리닉 정보가 없습니다." description="수강 중인 반이 없거나 배정 정보가 비어 있습니다." />
          )}

          {!isLoading && contextGroups.length > 0 && (
            <div className="grid gap-4 md:grid-cols-2">
              {contextGroups.map((group) => {
                const isSelected = group.key === selectedContextKey;
                return (
                  <button
                    key={group.key}
                    type="button"
                    onClick={() => handleSelectContext(group.key)}
                    className={clsx(
                      "rounded-2xl border p-4 text-left transition focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                      isSelected
                        ? "border-blue-200 bg-blue-50/70 shadow-sm"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">
                          {group.teacherName} · {group.branchName}
                        </p>
                        <p className="text-xs text-slate-500">{group.companyName}</p>
                      </div>
                      {isSelected && <Badge>선택됨</Badge>}
                    </div>
                    <p className="mt-3 text-xs text-slate-500">반 {group.courses.length}개</p>

                    {isSelected && group.courses.length > 1 && (
                      <div className="mt-3">
                        <Select
                          label="반 선택"
                          value={selectedCourseId ?? ""}
                          onChange={(event) => setSelectedCourseId(event.target.value)}
                        >
                          {group.courses.map((course) => (
                            <option key={course.courseId} value={course.courseId}>
                              {course.courseName}
                            </option>
                          ))}
                        </Select>
                      </div>
                    )}

                    {isSelected && group.courses.length === 1 && (
                      <p className="mt-3 text-sm font-semibold text-slate-700">
                        반: {group.courses[0]?.courseName}
                      </p>
                    )}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </Card>

      <Card
        title="기본 슬롯 시간표"
        description="선택한 반 기준으로 기본 슬롯을 설정하는 시간표가 이어집니다."
      >
        <div className="space-y-6">
          {selectedCourseContext ? (
            <div className="flex flex-wrap items-center gap-2 text-sm text-slate-600">
              <Badge variant="secondary">{selectedCourseContext.courseName ?? "선택된 반"}</Badge>
              <span>
                {selectedCourseContext.teacherName ?? "선생님"} · {selectedCourseContext.branchName ?? "지점"}
              </span>
              <span className="text-xs text-slate-400">기본 슬롯은 파란색으로 표시됩니다.</span>
            </div>
          ) : null}

          {!selectedCourseId && (
            <EmptyState message="반을 먼저 선택해 주세요." description="선생님/지점 선택 후 반이 정해져야 시간표가 표시됩니다." />
          )}

          {selectedCourseId && slotsError && (
            <div className="space-y-3">
              <InlineError message={slotsError} />
              <Button variant="secondary" onClick={() => void refreshSlots()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {selectedCourseId && slotsLoading && (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-[640px] w-full" />
            </div>
          )}

          {selectedCourseId && !slotsLoading && slots.length === 0 && !slotsError && (
            <EmptyState message="등록된 슬롯이 없습니다." description="선생님이 아직 슬롯을 등록하지 않았습니다." />
          )}

          {selectedCourseId && !slotsLoading && slots.length > 0 && !slotsError && (
            <div className="space-y-3">
              <p className="text-xs text-slate-500">슬롯을 클릭하면 기본 슬롯으로 설정할 수 있습니다.</p>
              <WeeklyTimeGrid
                days={GRID_DAYS}
                itemsByDay={slotsByDay}
                startHour={GRID_START_HOUR}
                endHour={GRID_END_HOUR}
                hourHeight={GRID_HOUR_HEIGHT}
                showDateHeader={false}
                getItemRange={(slot) => ({
                  startTime: slot.startTime,
                  endTime: slot.endTime
                })}
                getItemKey={(slot, index) => slot.slotId ?? `${slot.dayOfWeek}-${slot.startTime}-${index}`}
                renderItem={({ item, style }) => {
                  const isDefault = item.slotId && item.slotId === selectedCourseContext?.defaultClinicSlotId;
                  const isDisabled = isDefault || isUpdating;
                  return (
                    <button
                      type="button"
                      onClick={() => handleSlotSelect(item)}
                      disabled={isDisabled}
                      className={clsx(
                        "absolute left-1 right-1 rounded-2xl border px-2.5 py-2 text-left text-xs shadow-sm transition",
                        "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                        isDefault
                          ? "cursor-default border-blue-200 bg-blue-50/70 text-slate-700"
                          : "border-slate-200 bg-white text-slate-700 hover:border-slate-300",
                        isDisabled && "opacity-60"
                      )}
                      style={style}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold leading-tight">
                            {formatTime(item.startTime)} - {formatTime(item.endTime)}
                          </p>
                          <p className="text-[11px] text-slate-500">정원 {item.defaultCapacity ?? "-"}</p>
                        </div>
                        {isDefault && (
                          <span className="shrink-0 rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                            기본
                          </span>
                        )}
                      </div>
                    </button>
                  );
                }}
              />
            </div>
          )}
        </div>
      </Card>

      <Modal
        open={confirmOpen}
        onClose={closeConfirm}
        title={selectedCourseContext?.defaultClinicSlotId ? "기본 슬롯을 변경할까요?" : "기본 슬롯을 설정할까요?"}
        size="sm"
      >
        <div className="space-y-4 text-sm text-slate-600">
          <p>
            {selectedCourseContext?.defaultClinicSlotId
              ? "기존 기본 슬롯이 변경됩니다. 선택한 슬롯으로 기본 참석 기준이 바뀝니다."
              : "선택한 슬롯이 기본 참석 기준으로 설정됩니다."}
          </p>
          {pendingSlot ? (
            <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-slate-700">
              <p className="text-xs text-slate-500">선택한 슬롯</p>
              <p className="font-semibold">
                {getDayLabel(pendingSlot.dayOfWeek)} {formatTime(pendingSlot.startTime)} -{" "}
                {formatTime(pendingSlot.endTime)}
              </p>
            </div>
          ) : null}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeConfirm} disabled={isUpdating}>
              취소
            </Button>
            <Button onClick={() => void handleConfirmDefaultSlot()} disabled={isUpdating}>
              {isUpdating ? "변경 중..." : "확인"}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
