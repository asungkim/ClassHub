"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/shared/empty-state";
import { InlineError } from "@/components/ui/inline-error";
import type { StudentMyCourseResponse } from "@/types/dashboard";
import {
  DASHBOARD_PAGE_SIZE,
  fetchStudentMyCourses
} from "@/lib/dashboard-api";

export default function StudentMyCoursesPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return <StudentMyCoursesContent />;
}

function StudentMyCoursesContent() {
  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Student · My Courses</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">내 수업</h1>
        <p className="mt-2 text-sm text-slate-500">
          연결된 수업을 한 번에 확인할 수 있습니다. 아직 수업이 없다면 선생님 관리에서 연결 요청을 보내 주세요.
        </p>
      </header>
      <CoursesTab />
    </div>
  );
}

function CoursesTab() {
  const { showToast } = useToast();
  const [page, setPage] = useState(0);
  const [courses, setCourses] = useState<StudentMyCourseResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchStudentMyCourses({
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setCourses(result.items);
      setTotal(result.totalElements);
    } catch (err) {
      const message = err instanceof Error ? err.message : "수업 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [page, showToast]);

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  return (
    <div className="space-y-6">
      <Card
        title="연결된 수업"
        description="현재 연결된 Course 목록입니다. 휴원 상태도 함께 표시됩니다."
        actions={
          <Button variant="secondary" onClick={() => void loadCourses()} disabled={loading}>
            새로고침
          </Button>
        }
      >
        {error && <InlineError className="mb-4" message={error} />}
        {loading && <p className="py-12 text-center text-sm text-slate-500">수업을 불러오는 중입니다...</p>}
        {!loading && courses.length === 0 && (
          <div className="py-12">
            <EmptyState
              message="연결된 수업이 없습니다."
              description="선생님 관리에서 연결 요청을 보내면 승인 후 이곳에 표시됩니다."
            />
            <div className="mt-6 text-center">
              <Button asChild>
                <Link href="/student/teachers">선생님 관리로 이동</Link>
              </Button>
            </div>
          </div>
        )}
        {!loading && courses.length > 0 && (
          <div className="grid gap-4 lg:grid-cols-2">
            {courses.map((assignment, index) => {
              const course = assignment.course;
              const assignmentActive = assignment.assignmentActive ?? true;
              const progressStatus = getCourseProgressStatus(course?.startDate, course?.endDate);
              const key = assignment.assignmentId ?? `course-${index}`;
              return (
                <article
                  key={key}
                  className="rounded-2xl border border-slate-200 p-5 shadow-sm transition hover:border-indigo-200 hover:bg-indigo-50/40"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">
                        {formatAcademyName(course)}
                      </p>
                      <h3 className="mt-1 text-lg font-bold text-slate-900">{course?.name ?? "이름 없는 반"}</h3>
                      <p className="text-xs text-slate-500">
                        연결일 {formatDate(assignment.assignedAt)}
                      </p>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant={assignmentActive ? "success" : "secondary"}>
                        {assignmentActive ? "재원" : "휴원"}
                      </Badge>
                      <Badge variant={progressStatus.variant}>{progressStatus.label}</Badge>
                    </div>
                  </div>
                  <dl className="mt-4 space-y-2 text-sm text-slate-600">
                    <div className="flex gap-2">
                      <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">기간</dt>
                      <dd>{formatPeriod(course?.startDate, course?.endDate)}</dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">스케줄</dt>
                      <dd className="text-slate-500">{courseScheduleSummary(course)}</dd>
                    </div>
                  </dl>
                </article>
              );
            })}
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>
    </div>
  );
}

function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  disabled
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="mt-6 flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3 text-sm text-slate-600">
      <span>
        페이지 {currentPage + 1} / {totalPages}
      </span>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          className="h-9 px-4 text-xs"
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={disabled || currentPage === 0}
        >
          이전
        </Button>
        <Button
          variant="secondary"
          className="h-9 px-4 text-xs"
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={disabled || currentPage >= totalPages - 1}
        >
          다음
        </Button>
      </div>
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  try {
    return new Intl.DateTimeFormat("ko", { dateStyle: "medium" }).format(new Date(value));
  } catch {
    return value;
  }
}

function formatPeriod(start?: string | null, end?: string | null) {
  if (!start && !end) {
    return "일정 미정";
  }
  const fmt = (value?: string | null) => {
    if (!value) return "?";
    try {
      return new Intl.DateTimeFormat("ko", { dateStyle: "medium" }).format(new Date(value));
    } catch {
      return value;
    }
  };
  return `${fmt(start)} ~ ${fmt(end)}`;
}

function getCourseProgressStatus(start?: string | null, end?: string | null) {
  const today = startOfDay(new Date());
  const startDate = parseDateOnly(start);
  const endDate = parseDateOnly(end);

  if (!startDate && !endDate) {
    return { label: "일정 미정", variant: "secondary" as const };
  }
  if (startDate && today < startDate) {
    return { label: "진행 예정", variant: "default" as const };
  }
  if (endDate && today > endDate) {
    return { label: "종료", variant: "secondary" as const };
  }
  return { label: "진행 중", variant: "success" as const };
}

function parseDateOnly(value?: string | null) {
  if (!value) return null;
  const parsed = new Date(`${value}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  return parsed;
}

function startOfDay(date: Date) {
  const normalized = new Date(date);
  normalized.setHours(0, 0, 0, 0);
  return normalized;
}

function formatAcademyName(course?: StudentMyCourseResponse["course"]) {
  if (!course) return "학원 미지정";
  const company = course.companyName ?? "학원";
  const branch = course.branchName ?? "";
  return `${company} ${branch}`.trim();
}

function courseScheduleSummary(course?: StudentMyCourseResponse["course"]) {
  if (!course?.schedules || course.schedules.length === 0) {
    return "등록된 시간이 없습니다.";
  }
  return course.schedules
    .map((schedule: { dayOfWeek?: string | null; startTime?: string | null; endTime?: string | null }) =>
      `${weekdayLabel(schedule.dayOfWeek)} ${schedule.startTime ?? ""}~${schedule.endTime ?? ""}`
    )
    .join(", ");
}

function weekdayLabel(day?: string | null) {
  const map: Record<string, string> = {
    MONDAY: "월",
    TUESDAY: "화",
    WEDNESDAY: "수",
    THURSDAY: "목",
    FRIDAY: "금",
    SATURDAY: "토",
    SUNDAY: "일"
  };
  if (!day) return "?";
  return map[day] ?? day;
}
