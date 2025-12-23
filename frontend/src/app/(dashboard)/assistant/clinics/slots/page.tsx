"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicSlots } from "@/hooks/clinic/use-clinic-slots";
import { fetchAssistantCourses } from "@/lib/dashboard-api";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
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

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];

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
  const [contexts, setContexts] = useState<AssistantContextOption[]>([]);
  const [contextLoading, setContextLoading] = useState(false);
  const [contextError, setContextError] = useState<string | null>(null);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);

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
    const map = new Map<string, ClinicSlotResponse[]>();
    DAY_OPTIONS.forEach((day) => map.set(day.value, []));
    slots.forEach((slot) => {
      if (!slot.dayOfWeek) {
        return;
      }
      const list = map.get(slot.dayOfWeek) ?? [];
      list.push(slot);
      map.set(slot.dayOfWeek, list);
    });
    map.forEach((list) => {
      list.sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
    });
    return map;
  }, [slots]);

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
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {DAY_OPTIONS.slice(0, 3).map((day) => (
                <Skeleton key={day.value} className="h-40 w-full" />
              ))}
            </div>
          )}

          {!slotsLoading && !selectedContext && (
            <EmptyState message="선생님/지점을 먼저 선택해 주세요." description="선택 후 슬롯이 표시됩니다." />
          )}

          {!slotsLoading && selectedContext && slots.length === 0 && !slotsError && (
            <EmptyState message="등록된 슬롯이 없습니다." description="선생님이 슬롯을 등록하면 이곳에 표시됩니다." />
          )}

          {!slotsLoading && selectedContext && slots.length > 0 && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {DAY_OPTIONS.map((day) => {
                const daySlots = slotsByDay.get(day.value) ?? [];
                return (
                  <div key={day.value} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold text-slate-900">{day.label}</p>
                      <Badge variant="secondary">{daySlots.length}개</Badge>
                    </div>
                    {daySlots.length === 0 && (
                      <p className="mt-3 text-xs text-slate-400">등록된 슬롯이 없습니다.</p>
                    )}
                    {daySlots.map((slot) => (
                      <div
                        key={slot.slotId ?? `${day.value}-${slot.startTime}-${slot.endTime}`}
                        className="mt-3 rounded-xl border border-slate-200 bg-white px-3 py-2"
                      >
                        <p className="text-sm font-semibold text-slate-800">
                          {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                        </p>
                        <p className="text-xs text-slate-500">정원 {slot.defaultCapacity ?? "-"}</p>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
