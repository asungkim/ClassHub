"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
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
import { WeeklyTimeGrid } from "@/components/shared/weekly-time-grid";
import type { components } from "@/types/openapi";
import { fetchStudentMyCourses } from "@/lib/dashboard-api";
import type { StudentMyCourseResponse } from "@/types/dashboard";

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

type CourseStatus = {
  assignmentActive: boolean;
  courseActive: boolean;
};

const COURSE_STATUS_PAGE_SIZE = 50;

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
  const [selectedAttendingSessionId, setSelectedAttendingSessionId] = useState<string | null>(null);
  const [pendingAddSession, setPendingAddSession] = useState<ClinicSessionResponse | null>(null);
  const [pendingMoveSource, setPendingMoveSource] = useState<ClinicSessionResponse | null>(null);
  const [pendingMoveTarget, setPendingMoveTarget] = useState<ClinicSessionResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAddConfirmOpen, setIsAddConfirmOpen] = useState(false);
  const [isMoveConfirmOpen, setIsMoveConfirmOpen] = useState(false);
  const [selectedMobileDayKey, setSelectedMobileDayKey] = useState<string>(DAY_ORDER[0].value);
  const [mobileAddModalOpen, setMobileAddModalOpen] = useState(false);
  const [mobileMoveModalOpen, setMobileMoveModalOpen] = useState(false);
  const [mobileMoveSelectedSource, setMobileMoveSelectedSource] = useState<ClinicSessionResponse | null>(null);
  const [courseStatusMap, setCourseStatusMap] = useState<Map<string, CourseStatus>>(new Map());
  const [courseStatusLoading, setCourseStatusLoading] = useState(false);
  const [courseStatusError, setCourseStatusError] = useState<string | null>(null);

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

  const loadCourseStatuses = useCallback(async () => {
    setCourseStatusLoading(true);
    setCourseStatusError(null);
    try {
      let page = 0;
      let total = 0;
      const allCourses: StudentMyCourseResponse[] = [];
      while (page === 0 || allCourses.length < total) {
        const result = await fetchStudentMyCourses({ page, size: COURSE_STATUS_PAGE_SIZE });
        if (page === 0) {
          total = result.totalElements;
        }
        if (result.items.length === 0) {
          break;
        }
        allCourses.push(...result.items);
        page += 1;
      }
      const nextMap = new Map<string, CourseStatus>();
      allCourses.forEach((course) => {
        const courseId = course.course?.courseId;
        if (!courseId) {
          return;
        }
        nextMap.set(courseId, {
          assignmentActive: course.assignmentActive ?? true,
          courseActive: course.course?.active ?? true
        });
      });
      setCourseStatusMap(nextMap);
    } catch (err) {
      const message = err instanceof Error ? err.message : "수강 상태를 불러오지 못했습니다.";
      setCourseStatusError(message);
      showToast("error", message);
    } finally {
      setCourseStatusLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    void loadCourseStatuses();
  }, [loadCourseStatuses]);

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

  const selectedCourseStatus = useMemo(
    () => (selectedCourseId ? courseStatusMap.get(selectedCourseId) ?? null : null),
    [courseStatusMap, selectedCourseId]
  );
  const selectedCourseBlockedReason = useMemo(() => {
    if (!selectedCourseStatus || !selectedCourseId) {
      return null;
    }
    if (!selectedCourseStatus.assignmentActive) {
      return "휴원 상태라 클리닉 신청과 이동을 할 수 없습니다.";
    }
    if (!selectedCourseStatus.courseActive) {
      return "보관된 반이라 클리닉 신청과 이동을 할 수 없습니다.";
    }
    return null;
  }, [selectedCourseId, selectedCourseStatus]);
  const isSelectedCourseBlocked = Boolean(selectedCourseBlockedReason);

  useEffect(() => {
    if (!selectedAttendingSessionId) {
      return;
    }
    if (!attendanceSessionIds.has(selectedAttendingSessionId)) {
      setSelectedAttendingSessionId(null);
    }
  }, [attendanceSessionIds, selectedAttendingSessionId]);

  const sessionsByDay = useMemo(() => {
    const base: Record<string, ClinicSessionResponse[]> = {};
    DAY_ORDER.forEach((day) => {
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

  const closeAddConfirm = () => {
    setPendingAddSession(null);
    setIsAddConfirmOpen(false);
  };

  const closeMoveConfirm = () => {
    setPendingMoveSource(null);
    setPendingMoveTarget(null);
    setIsMoveConfirmOpen(false);
  };

  const handleSessionClick = (session: ClinicSessionResponse) => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 기능을 사용할 수 없습니다.");
      return;
    }
    if (!session.sessionId) {
      return;
    }

    const isAttending = attendanceSessionIds.has(session.sessionId);

    if (isAttending) {
      // 참석 중인 세션 클릭 → 이동 모드 활성화
      setSelectedAttendingSessionId(session.sessionId);
      return;
    }

    // 미참석 세션 클릭
    if (selectedAttendingSessionId) {
      // 이동 모드가 활성화된 상태 → 이동 확인 모달
      if (session.isCanceled) {
        showToast("info", "취소된 세션은 선택할 수 없습니다.");
        return;
      }
      if (isMoveLocked(session)) {
        showToast("info", "세션 시작 30분 전에는 이동할 수 없습니다.");
        return;
      }
      const sourceSession = sessions.find((s) => s.sessionId === selectedAttendingSessionId);
      if (sourceSession) {
        setPendingMoveSource(sourceSession);
        setPendingMoveTarget(session);
        setIsMoveConfirmOpen(true);
      }
    } else {
      // 이동 모드가 아닌 상태 → 추가 신청 모달
      if (session.isCanceled) {
        showToast("info", "취소된 세션에는 참석 신청을 할 수 없습니다.");
        return;
      }
      setPendingAddSession(session);
      setIsAddConfirmOpen(true);
    }
  };

  const handleRequestAttendance = async () => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 신청을 할 수 없습니다.");
      return;
    }
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
      await refreshSessions();
    } catch (err) {
      const message = err instanceof Error ? err.message : "참석 신청에 실패했습니다.";
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleMoveAttendance = async () => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 이동을 할 수 없습니다.");
      return;
    }
    if (!pendingMoveSource?.sessionId || !pendingMoveTarget?.sessionId) {
      return;
    }
    setIsSubmitting(true);
    try {
      await moveAttendance({
        fromSessionId: pendingMoveSource.sessionId,
        toSessionId: pendingMoveTarget.sessionId
      });
      showToast("success", "참석 세션이 변경되었습니다.");
      closeMoveConfirm();
      setSelectedAttendingSessionId(null);
      await refreshAttendances();
      await refreshSessions();
    } catch (err) {
      const message = err instanceof Error ? err.message : "세션 변경에 실패했습니다.";
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleMobileMoveOpen = (source: ClinicSessionResponse) => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 이동을 할 수 없습니다.");
      return;
    }
    setMobileMoveSelectedSource(source);
    setMobileMoveModalOpen(true);
  };

  const handleMobileMoveSelect = (target: ClinicSessionResponse) => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 이동을 할 수 없습니다.");
      return;
    }
    if (!mobileMoveSelectedSource) return;
    setPendingMoveSource(mobileMoveSelectedSource);
    setPendingMoveTarget(target);
    setMobileMoveModalOpen(false);
    setMobileMoveSelectedSource(null);
    setIsMoveConfirmOpen(true);
  };

  const handleMobileAddOpen = () => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 신청을 할 수 없습니다.");
      return;
    }
    setMobileAddModalOpen(true);
  };

  const handleMobileAddSelect = (session: ClinicSessionResponse) => {
    if (isSelectedCourseBlocked) {
      showToast("info", selectedCourseBlockedReason ?? "클리닉 신청을 할 수 없습니다.");
      return;
    }
    setPendingAddSession(session);
    setMobileAddModalOpen(false);
    setIsAddConfirmOpen(true);
  };

  if (!canRender) {
    return fallback;
  }

  const hasError = contextsError || sessionsError || attendancesError;
  const isLoading = contextsLoading || sessionsLoading || attendancesLoading;

  return (
    <div className="space-y-4 md:space-y-6 lg:space-y-8 overflow-x-hidden">
      <Card title="이번 주 클리닉" description={`이번 주 ${weekRangeLabel} 기준으로 세션을 표시합니다.`}>
        <div className="space-y-4 md:space-y-6 overflow-x-hidden">
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
                      "rounded-2xl border p-3 md:p-4 text-left transition focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                      isSelected
                        ? "border-blue-200 bg-blue-50/70 shadow-sm"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-slate-900 truncate">
                          {group.teacherName} · {group.branchName}
                        </p>
                        <p className="text-xs text-slate-500 truncate">{group.companyName}</p>
                      </div>
                      {isSelected && <Badge>선택됨</Badge>}
                    </div>
                    <p className="mt-2 md:mt-3 text-xs text-slate-500">반 {group.courses.length}개</p>

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
          {selectedCourseBlockedReason && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
              {selectedCourseBlockedReason}
            </div>
          )}
          {courseStatusError && <InlineError message={courseStatusError} />}
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
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-[640px] w-full" />
            </div>
          )}

          {!isLoading && !selectedContextGroup && (
            <EmptyState message="선생님/지점을 먼저 선택해 주세요." description="선택 후 이번 주 세션이 표시됩니다." />
          )}

          {!isLoading && selectedContextGroup && sessions.length === 0 && !sessionsError && (
            <EmptyState message="이번 주 세션이 없습니다." description="선생님이 이번 주 세션을 생성하지 않았습니다." />
          )}

          {!isLoading && selectedContextGroup && sessions.length > 0 && (
            <>
              {/* 데스크톱 뷰: 주간 그리드 */}
              <div className="hidden md:block space-y-3">
                {selectedAttendingSessionId ? (
                  <p className="text-xs text-slate-500">
                    이동할 세션을 시간표에서 선택하세요. 초록색 테두리는 이동 가능한 세션입니다.
                  </p>
                ) : (
                  <p className="text-xs text-slate-500">
                    참석 중인 세션을 클릭하면 이동 모드가 활성화됩니다. 미참석 세션을 클릭하면 추가 신청할 수 있습니다.
                  </p>
                )}
                <WeeklyTimeGrid
                days={GRID_DAYS}
                itemsByDay={sessionsByDay}
                startHour={GRID_START_HOUR}
                endHour={GRID_END_HOUR}
                hourHeight={GRID_HOUR_HEIGHT}
                rangeStart={weekRange.start}
                showDateHeader
                getItemRange={(session) => ({
                  startTime: session.startTime,
                  endTime: session.endTime
                })}
                getItemKey={(session, index) => session.sessionId ?? `${session.date}-${session.startTime}-${index}`}
                renderItem={({ item, style }) => {
                  const isAttending = item.sessionId ? attendanceSessionIds.has(item.sessionId) : false;
                  const isSelected = item.sessionId === selectedAttendingSessionId;
                  const isCanceled = Boolean(item.isCanceled);
                  const isLocked = isMoveLocked(item);
                  const isMoveCandidate = selectedAttendingSessionId && !isAttending && !isCanceled && !isLocked;
                  const attendanceCount = item.attendanceCount ?? 0;
                  const capacity = item.capacity ?? 0;
                  const isFull = !isCanceled && capacity > 0 && attendanceCount >= capacity;

                  const statusLabel = isCanceled
                    ? { text: "취소", className: "bg-slate-100 text-slate-500" }
                    : isAttending
                      ? { text: "참석", className: "bg-blue-100 text-blue-700" }
                      : isLocked
                        ? { text: "잠금", className: "bg-slate-100 text-slate-500" }
                        : null;
                  const fullLabel = isFull ? { text: "만석", className: "bg-rose-100 text-rose-700" } : null;

                  return (
                    <button
                      type="button"
                      onClick={() => handleSessionClick(item)}
                      disabled={isSelectedCourseBlocked || courseStatusLoading}
                      className={clsx(
                        "absolute left-1 right-1 rounded-2xl border px-2.5 py-2 text-left text-xs shadow-sm transition",
                        "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                        isCanceled
                          ? "border-slate-200 bg-slate-50 text-slate-400"
                          : isSelected
                            ? "border-indigo-300 bg-indigo-50 text-slate-700 ring-2 ring-indigo-200"
                            : isMoveCandidate
                              ? "border-emerald-300 bg-emerald-50 text-slate-700 ring-2 ring-emerald-200"
                              : isAttending
                                ? "border-blue-200 bg-blue-50 text-slate-700"
                                : "border-slate-200 bg-white text-slate-700",
                        (isSelectedCourseBlocked || courseStatusLoading) && "cursor-not-allowed opacity-60"
                      )}
                      style={style}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0 space-y-1">
                          <p className="text-xs font-semibold leading-tight break-words">
                            {formatTime(item.startTime)} ~ {formatTime(item.endTime)}
                          </p>
                          <p className="text-[10px] text-slate-500">
                            참석 {attendanceCount}/{capacity}
                          </p>
                        </div>
                        <div className="flex shrink-0 flex-col items-end gap-1">
                          {statusLabel && (
                            <span
                              className={clsx(
                                "rounded-full px-2 py-0.5 text-[10px] font-semibold",
                                statusLabel.className
                              )}
                            >
                              {statusLabel.text}
                            </span>
                          )}
                          {fullLabel && (
                            <span
                              className={clsx(
                                "rounded-full px-2 py-0.5 text-[10px] font-semibold",
                                fullLabel.className
                              )}
                            >
                              {fullLabel.text}
                            </span>
                          )}
                        </div>
                      </div>
                    </button>
                  );
                }}
              />
              </div>

              {/* 모바일 뷰: 요일 탭 + 세션 리스트 */}
              <div className="md:hidden space-y-3 overflow-x-hidden">
                <p className="text-xs text-slate-500">요일을 선택하면 해당 요일의 세션을 볼 수 있습니다.</p>

                {/* 요일 탭 */}
                <div className="flex gap-1.5 overflow-x-auto pb-2 -mx-2 px-2 scrollbar-hide">
                  {DAY_ORDER.map((day, index) => {
                    const date = addDays(weekRange.start, index);
                    const dayKey = getDayKeyFromDate(formatDate(date));
                    const daySessions = dayKey ? sessionsByDay[dayKey] ?? [] : [];
                    const hasSessions = daySessions.length > 0;
                    const hasAttendance = daySessions.some((s) => s.sessionId && attendanceSessionIds.has(s.sessionId));

                    return (
                      <button
                        key={day.value}
                        type="button"
                        onClick={() => setSelectedMobileDayKey(day.value)}
                        className={clsx(
                          "flex-shrink-0 rounded-full px-3 py-1.5 text-sm font-semibold transition",
                          "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                          selectedMobileDayKey === day.value
                            ? "bg-blue-100 text-blue-700"
                            : hasAttendance
                              ? "bg-blue-50 text-blue-600 hover:bg-blue-100"
                              : hasSessions
                                ? "bg-slate-100 text-slate-600 hover:bg-slate-200"
                                : "bg-slate-50 text-slate-400"
                        )}
                      >
                        {day.label}
                      </button>
                    );
                  })}
                </div>

                {/* 선택된 요일의 세션 리스트 */}
                {sessionsByDay[selectedMobileDayKey] && sessionsByDay[selectedMobileDayKey].length > 0 ? (
                  <div className="space-y-2">
                    {sessionsByDay[selectedMobileDayKey].map((session) => {
                      const isAttending = session.sessionId ? attendanceSessionIds.has(session.sessionId) : false;
                      const isCanceled = Boolean(session.isCanceled);
                      const isLocked = isMoveLocked(session);
                      const attendanceCount = session.attendanceCount ?? 0;
                      const capacity = session.capacity ?? 0;
                      const isFull = !isCanceled && capacity > 0 && attendanceCount >= capacity;

                      return (
                        <div
                          key={session.sessionId ?? `${session.date}-${session.startTime}`}
                          className={clsx(
                            "rounded-2xl border p-4 min-h-[100px]",
                            isCanceled
                              ? "border-slate-200 bg-slate-50"
                              : isAttending
                                ? "border-blue-200 bg-blue-50"
                                : "border-slate-200 bg-white"
                          )}
                        >
                          <div className="flex items-start justify-between gap-2 mb-3">
                            <div className="space-y-1.5 flex-1 min-w-0">
                              <p className="text-lg font-semibold text-slate-900">
                                {formatTime(session.startTime)} ~ {formatTime(session.endTime)}
                              </p>
                              <p className="text-sm text-slate-500">
                                참석 {attendanceCount}/{capacity}
                              </p>
                            </div>
                            <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                              {isAttending && <Badge>참석 중</Badge>}
                              {isCanceled && <Badge variant="destructive">취소</Badge>}
                              {isLocked && <Badge variant="secondary">잠금</Badge>}
                              {isFull && <Badge variant="destructive">만석</Badge>}
                            </div>
                          </div>

                          {!isCanceled && (
                            <div className="flex gap-2">
                              {isAttending ? (
                                <Button
                                  onClick={() => handleMobileMoveOpen(session)}
                                  disabled={isSubmitting || isSelectedCourseBlocked || courseStatusLoading}
                                  className="flex-1"
                                >
                                  이동
                                </Button>
                              ) : (
                                <Button
                                  variant="secondary"
                                  onClick={handleMobileAddOpen}
                                  disabled={isSubmitting || isSelectedCourseBlocked || courseStatusLoading}
                                  className="flex-1"
                                >
                                  추가 신청
                                </Button>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <EmptyState
                    message="이 요일은 등록된 세션이 없습니다."
                    description="다른 요일을 선택해주세요."
                  />
                )}
              </div>
            </>
          )}
        </div>
      </Card>

      <Modal open={isAddConfirmOpen} onClose={closeAddConfirm} title="해당 시간에 추가 신청 하시겠습니까?" size="sm">
        <div className="space-y-4 text-sm text-slate-600">
          <p>선택한 세션에 추가 참석을 신청합니다.</p>
          {pendingAddSession && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-slate-700">
              <p className="text-xs text-slate-500">선택한 세션</p>
              <p className="font-semibold">
                {getDayLabel(getDayKeyFromDate(pendingAddSession.date))} {formatTime(pendingAddSession.startTime)} -{" "}
                {formatTime(pendingAddSession.endTime)}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                참석 {pendingAddSession.attendanceCount ?? 0}/{pendingAddSession.capacity ?? 0}
              </p>
            </div>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeAddConfirm} disabled={isSubmitting}>
              취소
            </Button>
            <Button
              onClick={() => void handleRequestAttendance()}
              disabled={isSubmitting || isSelectedCourseBlocked || courseStatusLoading}
            >
              {isSubmitting ? "신청 중..." : "확인"}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal open={isMoveConfirmOpen} onClose={closeMoveConfirm} title="해당 시간으로 변경하시겠습니까?" size="sm">
        <div className="space-y-4 text-sm text-slate-600">
          <p>현재 참석 중인 세션에서 다른 세션으로 이동합니다.</p>
          {pendingMoveSource && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-slate-700">
              <p className="text-xs text-slate-500">현재 참석 중</p>
              <p className="font-semibold">
                {getDayLabel(getDayKeyFromDate(pendingMoveSource.date))} {formatTime(pendingMoveSource.startTime)} -{" "}
                {formatTime(pendingMoveSource.endTime)}
              </p>
            </div>
          )}
          {pendingMoveTarget && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-slate-700">
              <p className="text-xs text-emerald-600">이동할 세션</p>
              <p className="font-semibold">
                {getDayLabel(getDayKeyFromDate(pendingMoveTarget.date))} {formatTime(pendingMoveTarget.startTime)} -{" "}
                {formatTime(pendingMoveTarget.endTime)}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                참석 {pendingMoveTarget.attendanceCount ?? 0}/{pendingMoveTarget.capacity ?? 0}
              </p>
            </div>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeMoveConfirm} disabled={isSubmitting}>
              취소
            </Button>
            <Button
              onClick={() => void handleMoveAttendance()}
              disabled={isSubmitting || isSelectedCourseBlocked || courseStatusLoading}
            >
              {isSubmitting ? "변경 중..." : "확인"}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 모바일 이동 모달 */}
      <Modal
        open={mobileMoveModalOpen}
        onClose={() => {
          setMobileMoveModalOpen(false);
          setMobileMoveSelectedSource(null);
        }}
        title="이동할 시간 선택"
        size="md"
      >
        <div className="space-y-4 overflow-x-hidden">
          <p className="text-sm text-slate-600">이동 가능한 세션을 선택하세요.</p>

          <div className="space-y-3 max-h-96 overflow-y-auto overflow-x-hidden">
            {sessions
              .filter(
                (session) =>
                  session.sessionId &&
                  !attendanceSessionIds.has(session.sessionId) &&
                  !session.isCanceled &&
                  !isMoveLocked(session)
              )
              .map((session) => {
                const attendanceCount = session.attendanceCount ?? 0;
                const capacity = session.capacity ?? 0;
                const isFull = capacity > 0 && attendanceCount >= capacity;

                return (
                  <button
                    key={session.sessionId}
                    type="button"
                    onClick={() => handleMobileMoveSelect(session)}
                    disabled={isSubmitting || isSelectedCourseBlocked || courseStatusLoading}
                    className={clsx(
                      "w-full rounded-xl border p-3 text-left transition",
                      "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                      isFull
                        ? "border-rose-200 bg-rose-50"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:shadow-sm"
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1 flex-1 min-w-0">
                        <p className="text-sm font-semibold text-slate-900">
                          {getDayLabel(getDayKeyFromDate(session.date))} {formatTime(session.startTime)} ~{" "}
                          {formatTime(session.endTime)}
                        </p>
                        <p className="text-xs text-slate-500">
                          참석 {attendanceCount}/{capacity}
                        </p>
                      </div>
                      {isFull && <Badge variant="destructive">만석</Badge>}
                    </div>
                  </button>
                );
              })}
          </div>
        </div>
      </Modal>

      {/* 모바일 추가 모달 */}
      <Modal
        open={mobileAddModalOpen}
        onClose={() => setMobileAddModalOpen(false)}
        title="추가 신청할 시간 선택"
        size="md"
      >
        <div className="space-y-4 overflow-x-hidden">
          <p className="text-sm text-slate-600">추가 신청할 세션을 선택하세요.</p>

          <div className="space-y-3 max-h-96 overflow-y-auto overflow-x-hidden">
            {sessions
              .filter((session) => session.sessionId && !attendanceSessionIds.has(session.sessionId) && !session.isCanceled)
              .map((session) => {
                const attendanceCount = session.attendanceCount ?? 0;
                const capacity = session.capacity ?? 0;
                const isFull = capacity > 0 && attendanceCount >= capacity;

                return (
                  <button
                    key={session.sessionId}
                    type="button"
                    onClick={() => handleMobileAddSelect(session)}
                    disabled={isSubmitting || isSelectedCourseBlocked || courseStatusLoading}
                    className={clsx(
                      "w-full rounded-xl border p-3 text-left transition",
                      "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                      isFull
                        ? "border-rose-200 bg-rose-50"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:shadow-sm"
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1 flex-1 min-w-0">
                        <p className="text-sm font-semibold text-slate-900">
                          {getDayLabel(getDayKeyFromDate(session.date))} {formatTime(session.startTime)} ~{" "}
                          {formatTime(session.endTime)}
                        </p>
                        <p className="text-xs text-slate-500">
                          참석 {attendanceCount}/{capacity}
                        </p>
                      </div>
                      {isFull && <Badge variant="destructive">만석</Badge>}
                    </div>
                  </button>
                );
              })}
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

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}
