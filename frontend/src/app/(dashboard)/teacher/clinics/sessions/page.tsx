"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicSessions } from "@/hooks/clinic/use-clinic-sessions";
import { fetchTeacherBranchAssignments } from "@/lib/dashboard-api";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { TextField } from "@/components/ui/text-field";
import { TimeSelect } from "@/components/ui/time-select";
import { EmptyState } from "@/components/shared/empty-state";
import { WeeklyTimeGrid } from "@/components/shared/weekly-time-grid";
import type { components } from "@/types/openapi";
import type { TeacherBranchAssignment } from "@/types/dashboard";

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
const GRID_START_HOUR = 6;
const GRID_END_HOUR = 22;
const GRID_HOUR_HEIGHT = 56;

const GRID_DAYS = DAY_OPTIONS.map((day) => ({ key: day.value, label: day.label }));

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

const getDayKeyFromDate = (date?: string): DayKey | null => {
  if (!date) return null;
  const [year, month, day] = date.split("-").map(Number);
  if (!year || !month || !day) return null;
  const localDate = new Date(year, month - 1, day);
  return DAY_KEYS[localDate.getDay()] ?? null;
};

type ClinicSessionResponse = components["schemas"]["ClinicSessionResponse"];
type ClinicSessionEmergencyCreateRequest = components["schemas"]["ClinicSessionEmergencyCreateRequest"];
type WeekRange = { start: Date; end: Date };

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

export default function TeacherClinicSessionsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const { showToast } = useToast();
  const [branches, setBranches] = useState<TeacherBranchAssignment[]>([]);
  const [branchLoading, setBranchLoading] = useState(false);
  const [branchError, setBranchError] = useState<string | null>(null);
  const [selectedBranchId, setSelectedBranchId] = useState<string | null>(null);

  const [isEmergencyOpen, setIsEmergencyOpen] = useState(false);
  const [emergencyForm, setEmergencyForm] = useState<EmergencyFormState>(DEFAULT_EMERGENCY_FORM);
  const [emergencyError, setEmergencyError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [cancelTarget, setCancelTarget] = useState<ClinicSessionResponse | null>(null);
  const [isCanceling, setIsCanceling] = useState(false);

  const loadBranches = useCallback(async () => {
    setBranchLoading(true);
    setBranchError(null);
    try {
      const result = await fetchTeacherBranchAssignments({ status: "ACTIVE", page: 0, size: 100 });
      setBranches(result.items ?? []);
    } catch (err) {
      const message = err instanceof Error ? err.message : "지점 목록을 불러오지 못했습니다.";
      setBranchError(message);
    } finally {
      setBranchLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadBranches();
  }, [loadBranches]);

  useEffect(() => {
    if (branches.length === 0) {
      setSelectedBranchId(null);
      return;
    }
    if (selectedBranchId && branches.some((branch) => branch.branchId === selectedBranchId)) {
      return;
    }
    setSelectedBranchId(branches[0]?.branchId ?? null);
  }, [branches, selectedBranchId]);

  const selectedBranch = useMemo(
    () => branches.find((branch) => branch.branchId === selectedBranchId) ?? null,
    [branches, selectedBranchId]
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
    { dateRange: dateRangeValue, branchId: selectedBranchId ?? undefined },
    Boolean(selectedBranchId)
  );

  const sessionsByDay = useMemo(() => {
    const base: Record<string, ClinicSessionResponse[]> = {};
    DAY_OPTIONS.forEach((day) => {
      base[day.value] = [];
    });
    sessions.forEach((session) => {
      const dayKey = getDayKeyFromDate(session.date);
      if (!dayKey) return;
      base[dayKey] = [...(base[dayKey] ?? []), session];
    });
    Object.values(base).forEach((list) => {
      list.sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
    });
    return base;
  }, [sessions]);

  const openEmergencyModal = (preset?: Partial<EmergencyFormState>) => {
    setEmergencyForm({
      ...DEFAULT_EMERGENCY_FORM,
      date: formatDate(new Date()),
      ...preset
    });
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
    if (!selectedBranchId) {
      showToast("error", "지점을 먼저 선택해 주세요.");
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
        branchId: selectedBranchId,
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

  const handleSelectRange = (range: { date?: Date; startTime: string; endTime: string }) => {
    if (!selectedBranchId) {
      showToast("error", "지점을 먼저 선택해 주세요.");
      return;
    }
    if (!range.date) {
      openEmergencyModal();
      return;
    }
    openEmergencyModal({
      date: formatDate(range.date),
      startTime: range.startTime,
      endTime: range.endTime
    });
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
        title="주차별 클리닉"
        description={`${weekRangeLabel} 에 생성된 클리닉을 표시합니다.`}
      >
        <div className="space-y-6">

          {branchError && (
            <div className="space-y-3">
              <InlineError message={branchError} />
              <Button variant="secondary" onClick={() => void loadBranches()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {branchLoading && <Skeleton className="h-12 w-full max-w-sm" />}

          {!branchLoading && branches.length === 0 && (
            <EmptyState message="연결된 지점이 없습니다." description="지점 연결 후 세션을 관리할 수 있습니다." />
          )}

          {!branchLoading && branches.length > 0 && (
            <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
              <Select
                label="지점"
                value={selectedBranchId ?? ""}
                onChange={(event) => setSelectedBranchId(event.target.value)}
                className="md:w-80"
              >
                {branches.map((branch) => (
                  <option key={branch.branchId} value={branch.branchId}>
                    {branch.companyName ?? "학원"} · {branch.branchName ?? "지점"}
                  </option>
                ))}
              </Select>

              <Button onClick={() => openEmergencyModal()} disabled={!selectedBranchId}>
                긴급 세션 생성
              </Button>
            </div>
          )}
        </div>
      </Card>

      <Card title="주간 세션 시간표" description="선택한 지점 기준 이번주 클리닉을 표시합니다.">
        <div className="space-y-6">
          <div className="flex justify-center">
            <p className="text-sm font-semibold text-slate-700">{weekRangeLabel}</p>
          </div>

          {sessionsError && (
            <div className="space-y-3">
              <InlineError message={sessionsError} />
              <Button variant="secondary" onClick={() => void refreshSessions()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {sessionsLoading && (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-[640px] w-full" />
            </div>
          )}

          {!sessionsLoading && !selectedBranchId && (
            <EmptyState message="지점을 먼저 선택해 주세요." description="지점 선택 후 세션이 표시됩니다." />
          )}

          {!sessionsLoading && selectedBranchId && !sessionsError && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm text-slate-500">
                  셀을 드래그하거나 긴급 세션 생성 버튼을 통해 갑작스러운 상황에 대비하세요.
                </p>
                {sessions.length === 0 && (
                  <p className="text-sm font-semibold text-slate-700">이번 주 세션이 없습니다.</p>
                )}
              </div>
              <WeeklyTimeGrid
                days={GRID_DAYS}
                itemsByDay={sessionsByDay}
                startHour={GRID_START_HOUR}
                endHour={GRID_END_HOUR}
                hourHeight={GRID_HOUR_HEIGHT}
                rangeStart={weekRange.start}
                showDateHeader
                selectionEnabled={Boolean(selectedBranchId)}
                onSelectRange={handleSelectRange}
                getItemRange={(session) => ({
                  startTime: session.startTime,
                  endTime: session.endTime
                })}
                getItemKey={(session, index) => session.sessionId ?? `${session.date}-${session.startTime}-${index}`}
                renderItem={({ item, style }) => {
                  const isCanceled = Boolean(item.isCanceled);
                  const isEmergency = item.sessionType === "EMERGENCY";
                  return (
                    <div
                      className={clsx(
                        "absolute left-1 right-1 rounded-2xl border p-2 text-xs shadow-sm",
                        isCanceled
                          ? "border-slate-200 bg-slate-50 text-slate-400"
                          : isEmergency
                            ? "border-rose-200 bg-rose-50 text-rose-700"
                            : "border-blue-200 bg-blue-50 text-slate-700"
                      )}
                      style={style}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold">
                            {formatTime(item.startTime)} - {formatTime(item.endTime)}
                          </p>
                          <p className="text-[11px]">
                            {isEmergency ? "긴급" : "정규"} · 정원 {item.capacity ?? "-"}
                          </p>
                        </div>
                        {!isCanceled && (
                          <button
                            type="button"
                            className="shrink-0 text-[11px] font-semibold text-slate-500 hover:text-slate-700"
                            onClick={() => confirmCancel(item)}
                          >
                            취소
                          </button>
                        )}
                      </div>
                    </div>
                  );
                }}
              />
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
