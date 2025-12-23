"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicSessions } from "@/hooks/clinic/use-clinic-sessions";
import { fetchAssistantCourses } from "@/lib/dashboard-api";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { TextField } from "@/components/ui/text-field";
import { TimeSelect } from "@/components/ui/time-select";
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

const DAY_KEYS = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"] as const;

type DayKey = (typeof DAY_KEYS)[number];

const TIME_PLACEHOLDER = "--:--";

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

const getDayLabel = (value?: DayKey | null) =>
  DAY_OPTIONS.find((day) => day.value === value)?.label ?? value ?? "";

const getDayKeyFromDate = (date?: string): DayKey | null => {
  if (!date) return null;
  const [year, month, day] = date.split("-").map(Number);
  if (!year || !month || !day) return null;
  const localDate = new Date(year, month - 1, day);
  return DAY_KEYS[localDate.getDay()] ?? null;
};

type ClinicSessionResponse = components["schemas"]["ClinicSessionResponse"];
type ClinicSessionEmergencyCreateRequest = components["schemas"]["ClinicSessionEmergencyCreateRequest"];

type AssistantContextOption = {
  key: string;
  teacherId: string;
  teacherName: string;
  branchId: string;
  branchName: string;
  companyName: string;
};

type EmergencyFormState = {
  date: string;
  startTime: string;
  endTime: string;
  capacity: string;
};

const DEFAULT_EMERGENCY_FORM: EmergencyFormState = {
  date: formatDate(new Date()),
  startTime: "09:00",
  endTime: "10:00",
  capacity: "6"
};

export default function AssistantClinicSessionsPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  const { showToast } = useToast();
  const [contexts, setContexts] = useState<AssistantContextOption[]>([]);
  const [contextLoading, setContextLoading] = useState(false);
  const [contextError, setContextError] = useState<string | null>(null);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);

  const [isEmergencyOpen, setIsEmergencyOpen] = useState(false);
  const [emergencyForm, setEmergencyForm] = useState<EmergencyFormState>(DEFAULT_EMERGENCY_FORM);
  const [emergencyError, setEmergencyError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [cancelTarget, setCancelTarget] = useState<ClinicSessionResponse | null>(null);
  const [isCanceling, setIsCanceling] = useState(false);

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

  const weekRange = useMemo(() => getCurrentWeekRange(), []);
  const weekRangeLabel = useMemo(() => formatWeekRange(weekRange.start, weekRange.end), [weekRange]);
  const dateRangeValue = useMemo(() => formatDateRange(weekRange.start, weekRange.end), [weekRange]);

  const {
    sessions,
    isLoading: sessionsLoading,
    error: sessionsError,
    refresh: refreshSessions
  } = useClinicSessions(
    {
      dateRange: dateRangeValue,
      branchId: selectedContext?.branchId,
      teacherId: selectedContext?.teacherId
    },
    Boolean(selectedContext)
  );

  const sessionsByDay = useMemo(() => {
    const map = new Map<string, ClinicSessionResponse[]>();
    DAY_OPTIONS.forEach((day) => map.set(day.value, []));
    sessions.forEach((session) => {
      const dayKey = getDayKeyFromDate(session.date);
      if (!dayKey) return;
      const list = map.get(dayKey) ?? [];
      list.push(session);
      map.set(dayKey, list);
    });
    map.forEach((list) => {
      list.sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
    });
    return map;
  }, [sessions]);

  const openEmergencyModal = () => {
    setEmergencyForm({ ...DEFAULT_EMERGENCY_FORM, date: formatDate(new Date()) });
    setEmergencyError(null);
    setIsEmergencyOpen(true);
  };

  const closeEmergencyModal = () => {
    if (isSubmitting) {
      return;
    }
    setIsEmergencyOpen(false);
  };

  const submitEmergency = async () => {
    if (!selectedContext) {
      showToast("error", "선생님/지점을 먼저 선택해 주세요.");
      return;
    }
    if (!emergencyForm.date || !emergencyForm.startTime || !emergencyForm.endTime) {
      setEmergencyError("날짜와 시간을 모두 입력해주세요.");
      return;
    }
    if (emergencyForm.startTime >= emergencyForm.endTime) {
      setEmergencyError("종료 시간이 시작 시간보다 늦어야 합니다.");
      return;
    }
    const capacity = Number(emergencyForm.capacity);
    if (!Number.isFinite(capacity) || capacity < 1) {
      setEmergencyError("정원은 1명 이상이어야 합니다.");
      return;
    }

    setIsSubmitting(true);
    setEmergencyError(null);
    try {
      const body: ClinicSessionEmergencyCreateRequest = {
        branchId: selectedContext.branchId,
        teacherId: selectedContext.teacherId,
        date: emergencyForm.date,
        startTime: emergencyForm.startTime,
        endTime: emergencyForm.endTime,
        capacity
      };
      const response = await api.POST("/api/v1/clinic-sessions/emergency", { body });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "긴급 세션을 생성하지 못했습니다."));
      }
      if (!response.data?.data) {
        throw new Error("긴급 세션을 생성하지 못했습니다.");
      }
      showToast("success", "긴급 세션이 생성되었습니다.");
      setIsEmergencyOpen(false);
      await refreshSessions();
    } catch (err) {
      const message = err instanceof Error ? err.message : "긴급 세션을 생성하지 못했습니다.";
      setEmergencyError(message);
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const confirmCancel = (session: ClinicSessionResponse) => {
    setCancelTarget(session);
  };

  const handleCancel = async () => {
    if (!cancelTarget?.sessionId) {
      return;
    }
    setIsCanceling(true);
    try {
      const response = await api.PATCH("/api/v1/clinic-sessions/{sessionId}/cancel", {
        params: { path: { sessionId: cancelTarget.sessionId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "세션을 취소하지 못했습니다."));
      }
      showToast("success", "세션이 취소되었습니다.");
      setCancelTarget(null);
      await refreshSessions();
    } catch (err) {
      const message = err instanceof Error ? err.message : "세션을 취소하지 못했습니다.";
      showToast("error", message);
    } finally {
      setIsCanceling(false);
    }
  };

  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card
        title="주차별 클리닉 (세션)"
        description={`이번 주 ${weekRangeLabel} 기준으로 세션을 표시합니다.`}
      >
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">선생님/지점 선택</p>
            <p className="text-xs text-slate-500">선택한 선생님 기준으로 세션을 확인할 수 있습니다.</p>
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
            <EmptyState message="배정된 반이 없습니다." description="선생님 배정 후 세션을 확인할 수 있습니다." />
          )}

          {!contextLoading && contexts.length > 0 && (
            <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
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

              <Button onClick={openEmergencyModal} disabled={!selectedContext}>
                긴급 세션 생성
              </Button>
            </div>
          )}
        </div>
      </Card>

      <Card title="주간 세션 시간표" description="선택한 선생님 기준 주간 세션을 표시합니다.">
        <div className="space-y-6">
          {sessionsError && (
            <div className="space-y-3">
              <InlineError message={sessionsError} />
              <Button variant="secondary" onClick={() => void refreshSessions()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {sessionsLoading && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {DAY_OPTIONS.slice(0, 3).map((day) => (
                <Skeleton key={day.value} className="h-40 w-full" />
              ))}
            </div>
          )}

          {!sessionsLoading && !selectedContext && (
            <EmptyState message="선생님/지점을 먼저 선택해 주세요." description="선택 후 세션이 표시됩니다." />
          )}

          {!sessionsLoading && selectedContext && sessions.length === 0 && !sessionsError && (
            <EmptyState message="이번 주 세션이 없습니다." description="긴급 세션 생성으로 일정을 추가할 수 있습니다." />
          )}

          {!sessionsLoading && selectedContext && sessions.length > 0 && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {DAY_OPTIONS.map((day) => {
                const daySessions = sessionsByDay.get(day.value) ?? [];
                return (
                  <div key={day.value} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold text-slate-900">{day.label}</p>
                      <Badge variant="secondary">{daySessions.length}개</Badge>
                    </div>
                    {daySessions.length === 0 && (
                      <p className="mt-3 text-xs text-slate-400">등록된 세션이 없습니다.</p>
                    )}
                    {daySessions.map((session) => {
                      const isCanceled = Boolean(session.isCanceled);
                      return (
                        <div
                          key={session.sessionId ?? `${day.value}-${session.startTime}-${session.endTime}`}
                          className={clsx(
                            "mt-3 rounded-xl border px-3 py-2",
                            isCanceled ? "border-slate-200 bg-slate-50" : "border-slate-200 bg-white"
                          )}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <p className="text-sm font-semibold text-slate-800">
                              {formatTime(session.startTime)} - {formatTime(session.endTime)}
                            </p>
                            <div className="flex items-center gap-2">
                              <Badge variant={session.sessionType === "EMERGENCY" ? "destructive" : "secondary"}>
                                {session.sessionType === "EMERGENCY" ? "긴급" : "정규"}
                              </Badge>
                              {isCanceled && <Badge variant="secondary">취소됨</Badge>}
                            </div>
                          </div>
                          <p className="text-xs text-slate-500">
                            {getDayLabel(getDayKeyFromDate(session.date))} · 정원 {session.capacity ?? "-"}
                          </p>
                          <div className="mt-2 flex justify-end">
                            {!isCanceled && (
                              <Button variant="ghost" onClick={() => confirmCancel(session)}>
                                취소
                              </Button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </Card>

      <Modal open={isEmergencyOpen} onClose={closeEmergencyModal} title="긴급 세션 생성" size="sm">
        <div className="space-y-4">
          <TextField
            label="날짜"
            type="date"
            value={emergencyForm.date}
            onChange={(event) => setEmergencyForm((prev) => ({ ...prev, date: event.target.value }))}
          />
          <TimeSelect
            label="시작 시간"
            value={emergencyForm.startTime}
            onChange={(value) => setEmergencyForm((prev) => ({ ...prev, startTime: value }))}
          />
          <TimeSelect
            label="종료 시간"
            value={emergencyForm.endTime}
            onChange={(value) => setEmergencyForm((prev) => ({ ...prev, endTime: value }))}
          />
          <TextField
            label="정원"
            type="number"
            min={1}
            value={emergencyForm.capacity}
            onChange={(event) => setEmergencyForm((prev) => ({ ...prev, capacity: event.target.value }))}
          />

          {emergencyError && <InlineError message={emergencyError} />}

          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={closeEmergencyModal} disabled={isSubmitting}>
              취소
            </Button>
            <Button onClick={() => void submitEmergency()} disabled={isSubmitting}>
              {isSubmitting ? "생성 중..." : "생성"}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        onClose={() => setCancelTarget(null)}
        onConfirm={() => void handleCancel()}
        title="세션을 취소할까요?"
        message="취소한 세션은 되돌릴 수 없습니다."
        confirmText={isCanceling ? "취소 중..." : "취소"}
        isLoading={isCanceling}
      />
    </div>
  );
}

function getCurrentWeekRange(baseDate: Date = new Date()) {
  const base = new Date(baseDate);
  const day = base.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  const start = new Date(base);
  start.setDate(base.getDate() + diffToMonday);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(start.getDate() + 6);
  end.setHours(23, 59, 59, 999);
  return { start, end };
}

function formatWeekRange(start: Date, end: Date) {
  const formatter = new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit"
  });
  return `${formatter.format(start)} ~ ${formatter.format(end)}`;
}

function formatDateRange(start: Date, end: Date) {
  return `${formatDate(start)},${formatDate(end)}`;
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}
