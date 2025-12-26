"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/shared/empty-state";
import { InlineError } from "@/components/ui/inline-error";
import type { StudentCourseResponse } from "@/types/dashboard";
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
          승인된 수업을 한 번에 확인할 수 있습니다. 아직 수업이 없다면 선생님 관리에서 연결 요청을 보내 주세요.
        </p>
      </header>
      <CoursesTab />
    </div>
  );
}

function CoursesTab() {
  const { showToast } = useToast();
  const [keywordInput, setKeywordInput] = useState("");
  const keyword = useDebounce(keywordInput.trim(), 300);
  const [page, setPage] = useState(0);
  const [courses, setCourses] = useState<StudentCourseResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchStudentMyCourses({
        keyword: keyword || undefined,
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
  }, [keyword, page, showToast]);

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const resetFilters = () => {
    setKeywordInput("");
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <Card
        title="수업 검색"
        description="승인된 수업 중 이름으로 빠르게 찾을 수 있습니다. 필터가 비어 있으면 전체를 보여줍니다."
      >
        <div className="flex flex-col gap-4 md:flex-row md:items-end">
          <Field label="반 이름 검색" className="md:flex-1">
            <Input
              placeholder="예: 수학 심화반"
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
                setPage(0);
              }}
            />
          </Field>
          <div className="flex gap-3">
            <Button variant="secondary" onClick={resetFilters} disabled={loading}>
              초기화
            </Button>
            <Button onClick={() => void loadCourses()} disabled={loading}>
              새로고침
            </Button>
          </div>
        </div>
        {error && <InlineError className="mt-4" message={error} />}
      </Card>

      <Card title="연결된 수업" description="승인되어 수강 중인 Course 목록입니다.">
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
            {courses.map((enrollment, index) => {
              const course = enrollment.course;
              const key = enrollment.enrollmentId ?? `course-${index}`;
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
                        등록일 {formatDate(enrollment.enrolledAt)} · ID {enrollment.enrollmentId ?? "-"}
                      </p>
                    </div>
                    <Badge variant={course?.active ? "success" : "secondary"}>
                      {course?.active ? "진행 중" : "비활성"}
                    </Badge>
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

type FieldProps = {
  label: string;
  className?: string;
  children: ReactNode;
};

function Field({ label, className, children }: FieldProps) {
  return (
    <label className={`flex w-full flex-col gap-1 text-sm font-semibold text-slate-700 ${className ?? ""}`}>
      <span>{label}</span>
      {children}
    </label>
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

function formatAcademyName(course?: StudentCourseResponse["course"]) {
  if (!course) return "학원 미지정";
  const company = course.companyName ?? "학원";
  const branch = course.branchName ?? "";
  return `${company} ${branch}`.trim();
}

function courseScheduleSummary(course?: StudentCourseResponse["course"]) {
  if (!course?.schedules || course.schedules.length === 0) {
    return "등록된 시간이 없습니다.";
  }
  return course.schedules
    .map((schedule) => `${weekdayLabel(schedule.dayOfWeek)} ${schedule.startTime ?? ""}~${schedule.endTime ?? ""}`)
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
