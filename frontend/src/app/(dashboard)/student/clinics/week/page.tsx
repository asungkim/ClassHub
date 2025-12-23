"use client";

import { useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicContexts } from "@/hooks/clinic/use-clinic-contexts";
import { useClinicSessions } from "@/hooks/clinic/use-clinic-sessions";
import { useStudentAttendances } from "@/hooks/clinic/use-student-attendances";
import { useAttendanceMutations } from "@/hooks/clinic/use-attendance-mutations";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { InlineError } from "@/components/ui/inline-error";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { EmptyState } from "@/components/shared/empty-state";
import type { components } from "@/types/openapi";

type ClinicContextCourse = {
  courseId: string;
  courseName: string;
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

type ClinicSessionResponse = components["schemas"]["ClinicSessionResponse"];

const DAY_ORDER = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" }
];
const DAY_KEYS = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"] as const;
type DayKey = (typeof DAY_KEYS)[number];
const MOVE_LOCK_MINUTES = 30;

const TIME_PLACEHOLDER = "--:--";

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

const getDayLabel = (value?: DayKey | null) =>
  DAY_ORDER.find((day) => day.value === value)?.label ?? value ?? "";

const getDayKeyFromDate = (date?: string): DayKey | null => {
  if (!date) return null;
  const [year, month, day] = date.split("-").map(Number);
  if (!year || !month || !day) return null;
  const localDate = new Date(year, month - 1, day);
  return DAY_KEYS[localDate.getDay()] ?? null;
};

const getSessionStartAt = (session: ClinicSessionResponse): Date | null => {
  if (!session.date || !session.startTime) {
    return null;
  }
  const [year, month, day] = session.date.split("-").map(Number);
  const [hour, minute] = session.startTime.split(":").map(Number);
  if (!year || !month || !day || Number.isNaN(hour) || Number.isNaN(minute)) {
    return null;
  }
  return new Date(year, month - 1, day, hour, minute);
};

const isMoveLocked = (session: ClinicSessionResponse) => {
  const startAt = getSessionStartAt(session);
  if (!startAt) {
    return false;
  }
  const diffMs = startAt.getTime() - Date.now();
  return diffMs <= MOVE_LOCK_MINUTES * 60 * 1000;
};

export default function StudentClinicWeekPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  const { showToast } = useToast();
  const weekRange = useMemo(() => getCurrentWeekRange(), []);
  const weekRangeLabel = useMemo(() => formatWeekRange(weekRange.start, weekRange.end), [weekRange]);
  const dateRangeValue = useMemo(() => formatDateRange(weekRange.start, weekRange.end), [weekRange]);

  const { contexts, isLoading: contextsLoading, error: contextsError, refresh: refreshContexts } = useClinicContexts();
  const [selectedContextKey, setSelectedContextKey] = useState<string | null>(null);
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [selectedSession, setSelectedSession] = useState<ClinicSessionResponse | null>(null);
  const [isMoveMode, setIsMoveMode] = useState(false);
  const [moveSourceSessionId, setMoveSourceSessionId] = useState<string | null>(null);
  const [pendingAddSession, setPendingAddSession] = useState<ClinicSessionResponse | null>(null);
  const [pendingMoveTarget, setPendingMoveTarget] = useState<ClinicSessionResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAddConfirmOpen, setIsAddConfirmOpen] = useState(false);
  const [isMoveConfirmOpen, setIsMoveConfirmOpen] = useState(false);

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
        courseName: context.courseName ?? "반 이름 없음"
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
      setSelectedContextKey(null);
      setSelectedCourseId(null);
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

  const { requestAttendance, moveAttendance } = useAttendanceMutations();

  const selectedContextGroup = useMemo(
    () => contextGroups.find((group) => group.key === selectedContextKey) ?? null,
    [contextGroups, selectedContextKey]
  );

  const {
    sessions,
    isLoading: sessionsLoading,
    error: sessionsError,
    refresh: refreshSessions
  } = useClinicSessions(
    {
      dateRange: dateRangeValue,
      branchId: selectedContextGroup?.branchId,
      teacherId: selectedContextGroup?.teacherId
    },
    Boolean(selectedContextGroup)
  );

  const {
    attendances,
    isLoading: attendancesLoading,
    error: attendancesError,
    refresh: refreshAttendances
  } = useStudentAttendances(dateRangeValue, Boolean(dateRangeValue));

  const attendanceSessionIds = useMemo(() => {
    const set = new Set<string>();
    attendances.forEach((attendance) => {
      if (attendance.clinicSessionId) {
        set.add(attendance.clinicSessionId);
      }
    });
    return set;
  }, [attendances]);

  useEffect(() => {
    if (!isMoveMode || !moveSourceSessionId) {
      return;
    }
    if (!attendanceSessionIds.has(moveSourceSessionId)) {
      setIsMoveMode(false);
      setMoveSourceSessionId(null);
    }
  }, [attendanceSessionIds, isMoveMode, moveSourceSessionId]);

  const sessionsByDay = useMemo(() => {
    const map = new Map<string, ClinicSessionResponse[]>();
    DAY_ORDER.forEach((day) => map.set(day.value, []));
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

  const isSelectedAttending = Boolean(selectedSession?.sessionId && attendanceSessionIds.has(selectedSession.sessionId));
  const isSelectedCanceled = Boolean(selectedSession?.isCanceled);

  const resetMoveMode = () => {
    setIsMoveMode(false);
    setMoveSourceSessionId(null);
  };

  const openAddConfirm = (session: ClinicSessionResponse) => {
    setPendingAddSession(session);
    setIsAddConfirmOpen(true);
  };

  const closeAddConfirm = () => {
    setPendingAddSession(null);
    setIsAddConfirmOpen(false);
  };

  const openMoveConfirm = (session: ClinicSessionResponse) => {
    setPendingMoveTarget(session);
    setIsMoveConfirmOpen(true);
  };

  const closeMoveConfirm = () => {
    setPendingMoveTarget(null);
    setIsMoveConfirmOpen(false);
  };

  const handleSessionClick = (session: ClinicSessionResponse) => {
    if (!session.sessionId) {
      return;
    }

    if (isMoveMode) {
      if (session.sessionId === moveSourceSessionId) {
        showToast("info", "현재 참석 중인 세션입니다.");
        return;
      }
      if (attendanceSessionIds.has(session.sessionId)) {
        showToast("info", "이미 참석 중인 세션입니다.");
        return;
      }
      if (session.isCanceled) {
        showToast("info", "취소된 세션은 선택할 수 없습니다.");
        return;
      }
      if (isMoveLocked(session)) {
        showToast("info", "세션 시작 30분 전에는 이동할 수 없습니다.");
        return;
      }
      openMoveConfirm(session);
      return;
    }

    setSelectedSession(session);
  };

  const handleStartMoveMode = () => {
    if (!selectedSession?.sessionId) {
      return;
    }
    setIsMoveMode(true);
    setMoveSourceSessionId(selectedSession.sessionId);
    showToast("info", "이동할 세션을 시간표에서 선택하세요.");
  };

  const handleRequestAttendance = async () => {
    if (!pendingAddSession?.sessionId || !selectedCourseId) {
      showToast("error", "반 선택 후 참석을 신청할 수 있습니다.");
      return;
    }
    setIsSubmitting(true);
    try {
      await requestAttendance({
        clinicSessionId: pendingAddSession.sessionId,
        courseId: selectedCourseId
      });
      showToast("success", "참석 신청이 완료되었습니다.");
      closeAddConfirm();
      await refreshAttendances();
    } catch (err) {
      const message = err instanceof Error ? err.message : "참석 신청에 실패했습니다.";
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleMoveAttendance = async () => {
    if (!pendingMoveTarget?.sessionId || !moveSourceSessionId) {
      return;
    }
    setIsSubmitting(true);
    try {
      await moveAttendance({
        fromSessionId: moveSourceSessionId,
        toSessionId: pendingMoveTarget.sessionId
      });
      showToast("success", "참석 세션이 변경되었습니다.");
      closeMoveConfirm();
      resetMoveMode();
      setSelectedSession(pendingMoveTarget);
      await refreshAttendances();
    } catch (err) {
      const message = err instanceof Error ? err.message : "세션 변경에 실패했습니다.";
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!canRender) {
    return fallback;
  }

  const hasError = contextsError || sessionsError || attendancesError;
  const isLoading = contextsLoading || sessionsLoading || attendancesLoading;

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="이번 주 클리닉" description={`이번 주 ${weekRangeLabel} 기준으로 세션을 표시합니다.`}>
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">선생님/지점 선택</p>
            <p className="text-xs text-slate-500">선택한 선생님 기준으로 이번 주 세션을 보여줍니다.</p>
          </div>

          {contextsError && (
            <div className="space-y-3">
              <InlineError message={contextsError} />
              <Button variant="secondary" onClick={() => void refreshContexts()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {contextsLoading && (
            <div className="grid gap-4 md:grid-cols-2">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-28 w-full" />
            </div>
          )}

          {!contextsLoading && contextGroups.length === 0 && (
            <EmptyState message="표시할 클리닉 정보가 없습니다." description="수강 중인 반이 없거나 배정 정보가 비어 있습니다." />
          )}

          {!contextsLoading && contextGroups.length > 0 && (
            <div className="grid gap-4 md:grid-cols-2">
              {contextGroups.map((group) => {
                const isSelected = group.key === selectedContextKey;
                return (
                  <button
                    key={group.key}
                    type="button"
                    onClick={() => setSelectedContextKey(group.key)}
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
        title="주간 세션 시간표"
        description="이번 주 참석/비참석 세션을 시간표로 확인합니다."
      >
        <div className="space-y-6">
          {hasError && !contextsError && (
            <div className="space-y-3">
              <InlineError message={sessionsError ?? attendancesError ?? "세션을 불러오지 못했습니다."} />
              <Button
                variant="secondary"
                onClick={() => {
                  void refreshSessions();
                  void refreshAttendances();
                }}
                disabled={!selectedContextGroup}
              >
                다시 불러오기
              </Button>
            </div>
          )}

          {isLoading && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {DAY_ORDER.slice(0, 3).map((day) => (
                <Skeleton key={day.value} className="h-40 w-full" />
              ))}
            </div>
          )}

          {!isLoading && !selectedContextGroup && (
            <EmptyState message="선생님/지점을 먼저 선택해 주세요." description="선택 후 이번 주 세션이 표시됩니다." />
          )}

          {!isLoading && selectedContextGroup && sessions.length === 0 && !sessionsError && (
            <EmptyState message="이번 주 세션이 없습니다." description="선생님이 이번 주 세션을 생성하지 않았습니다." />
          )}

          {!isLoading && selectedContextGroup && sessions.length > 0 && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {DAY_ORDER.map((day) => {
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
                      const isAttending = session.sessionId ? attendanceSessionIds.has(session.sessionId) : false;
                      const isLocked = isMoveMode && isMoveLocked(session);
                      const isMoveSource = isMoveMode && session.sessionId === moveSourceSessionId;
                      const isMoveCandidate = isMoveMode && !isAttending && !session.isCanceled && !isLocked;
                      const isMoveDisabled = isMoveMode && !isMoveSource && !isMoveCandidate;
                      return (
                        <button
                          key={session.sessionId ?? `${day.value}-${session.startTime}-${session.endTime}`}
                          type="button"
                          onClick={() => handleSessionClick(session)}
                          disabled={isMoveMode && isMoveDisabled}
                          className={clsx(
                            "mt-3 w-full rounded-xl border px-3 py-2 text-left transition",
                            "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                            isAttending ? "border-blue-200 bg-blue-50/70" : "border-slate-200 bg-white",
                            isMoveCandidate && "border-emerald-200 bg-emerald-50/70",
                            isMoveSource && "border-indigo-200 bg-indigo-50/70",
                            isMoveDisabled && "cursor-not-allowed opacity-60"
                          )}
                        >
                          <div className="flex items-center justify-between">
                            <p className="text-sm font-semibold text-slate-800">
                              {formatTime(session.startTime)} - {formatTime(session.endTime)}
                            </p>
                            {isMoveSource && <Badge>변경 대상</Badge>}
                            {!isMoveSource && isAttending && <Badge>참석 중</Badge>}
                            {!isAttending && session.isCanceled && <Badge variant="destructive">취소</Badge>}
                            {!isAttending && !session.isCanceled && isLocked && (
                              <Badge variant="secondary">잠금</Badge>
                            )}
                          </div>
                          <p className="text-xs text-slate-500">
                            {getDayLabel(getDayKeyFromDate(session.date))} · 정원 {session.capacity ?? "-"}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                );
              })}
            </div>
          )}

          {!isLoading && selectedSession && (
            <div className="rounded-2xl border border-slate-200 bg-white p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-900">선택한 세션</p>
                  <p className="text-xs text-slate-500">
                    {getDayLabel(getDayKeyFromDate(selectedSession.date))} ·{" "}
                    {formatTime(selectedSession.startTime)} - {formatTime(selectedSession.endTime)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {isSelectedCanceled && <Badge variant="destructive">취소</Badge>}
                  {isSelectedAttending && !isMoveMode && <Badge>참석 중</Badge>}
                </div>
              </div>

              <div className="mt-3 text-xs text-slate-500">
                정원 {selectedSession.capacity ?? "-"} · {selectedContextGroup?.teacherName ?? "선생님"} 수업
              </div>

              <div className="mt-4">
                {isMoveMode ? (
                  <div className="flex flex-col gap-3">
                    <p className="text-sm text-slate-600">
                      이동할 세션을 시간표에서 선택하세요. 시작 30분 전 세션과 취소된 세션은 선택할 수 없습니다.
                    </p>
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={resetMoveMode} disabled={isSubmitting}>
                        변경 모드 취소
                      </Button>
                    </div>
                  </div>
                ) : isSelectedAttending ? (
                  <div className="flex flex-wrap gap-2">
                    <Button onClick={handleStartMoveMode} disabled={isSubmitting}>
                      변경 모드 시작
                    </Button>
                  </div>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    <Button onClick={() => openAddConfirm(selectedSession)} disabled={isSubmitting || isSelectedCanceled}>
                      추가 참석 신청
                    </Button>
                    {isSelectedCanceled && (
                      <p className="text-xs text-rose-500">취소된 세션에는 참석 신청을 할 수 없습니다.</p>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </Card>

      <Modal open={isAddConfirmOpen} onClose={closeAddConfirm} title="참석을 신청할까요?" size="sm">
        <div className="space-y-4 text-sm text-slate-600">
          <p>선택한 세션에 추가 참석을 신청합니다.</p>
          {pendingAddSession ? (
            <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-slate-700">
              <p className="text-xs text-slate-500">선택한 세션</p>
              <p className="font-semibold">
                {getDayLabel(getDayKeyFromDate(pendingAddSession.date))} {formatTime(pendingAddSession.startTime)} -{" "}
                {formatTime(pendingAddSession.endTime)}
              </p>
            </div>
          ) : null}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeAddConfirm} disabled={isSubmitting}>
              취소
            </Button>
            <Button onClick={() => void handleRequestAttendance()} disabled={isSubmitting}>
              {isSubmitting ? "신청 중..." : "확인"}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal open={isMoveConfirmOpen} onClose={closeMoveConfirm} title="세션을 변경할까요?" size="sm">
        <div className="space-y-4 text-sm text-slate-600">
          <p>현재 참석 중인 세션을 다른 세션으로 이동합니다.</p>
          {pendingMoveTarget ? (
            <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-slate-700">
              <p className="text-xs text-slate-500">이동할 세션</p>
              <p className="font-semibold">
                {getDayLabel(getDayKeyFromDate(pendingMoveTarget.date))} {formatTime(pendingMoveTarget.startTime)} -{" "}
                {formatTime(pendingMoveTarget.endTime)}
              </p>
            </div>
          ) : null}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeMoveConfirm} disabled={isSubmitting}>
              취소
            </Button>
            <Button onClick={() => void handleMoveAttendance()} disabled={isSubmitting}>
              {isSubmitting ? "변경 중..." : "확인"}
            </Button>
          </div>
        </div>
      </Modal>
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
