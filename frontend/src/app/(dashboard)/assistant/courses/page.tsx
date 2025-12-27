"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import clsx from "clsx";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/shared/empty-state";
import { InlineError } from "@/components/ui/inline-error";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/toast";
import type { CourseStatusFilter, CourseWithTeacherResponse } from "@/types/dashboard";
import { DASHBOARD_PAGE_SIZE, fetchAssistantCourses } from "@/lib/dashboard-api";

const statusOptions: { value: CourseStatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "ACTIVE", label: "활성" },
  { value: "INACTIVE", label: "비활성" }
];

type TeacherOption = { value: string; label: string };

export default function AssistantCoursesPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }
  return <AssistantCourseContent />;
}

function AssistantCourseContent() {
  const { showToast } = useToast();
  const [status, setStatus] = useState<CourseStatusFilter>("ALL");
  const [teacherId, setTeacherId] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const keyword = useDebounce(keywordInput.trim(), 300);
  const [page, setPage] = useState(0);

  const [courses, setCourses] = useState<CourseWithTeacherResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [teacherOptions, setTeacherOptions] = useState<TeacherOption[]>([]);

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAssistantCourses({
        teacherId: teacherId || undefined,
        status,
        keyword: keyword || undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setCourses(result.items);
      setTotal(result.totalElements);
      if (result.items.length > 0) {
        setTeacherOptions((prev) => {
          const map = new Map(prev.map((opt) => [opt.value, opt.label] as const));
          result.items.forEach((course) => {
            if (course.teacherId) {
              map.set(course.teacherId, course.teacherName ?? "이름 없음");
            }
          });
          return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
        });
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "배정된 반 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [teacherId, status, keyword, page, showToast]);

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const resetFilters = () => {
    setTeacherId("");
    setKeywordInput("");
    setStatus("ALL");
    setPage(0);
  };

  const formatPeriod = (course: CourseWithTeacherResponse) => {
    if (!course.startDate || !course.endDate) return "-";
    return `${course.startDate} ~ ${course.endDate}`;
  };

  const scheduleSummary = (course: CourseWithTeacherResponse) => {
    if (!course.schedules || course.schedules.length === 0) {
      return "등록된 스케줄이 없습니다.";
    }
    return course.schedules
      .map((schedule) => `${weekdayLabel(schedule.dayOfWeek)} ${schedule.startTime}~${schedule.endTime}`)
      .join(", ");
  };

  const emptyDescription = useMemo(() => {
    switch (status) {
      case "ACTIVE":
        return "활성 반이 아직 없어요. 담당 선생님에게 확인해 주세요.";
      case "INACTIVE":
        return "비활성 반이 없습니다.";
      default:
        return "배정 받은 반이 없습니다.";
    }
  }, [status]);

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Assistant · Courses</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">반 목록</h1>
        <p className="mt-2 text-sm text-slate-500">
          연결된 선생님의 반을 확인하고, 수업 준비 혹은 클리닉 업무에 참고하세요.
        </p>
      </header>

      <Card title="필터" description="선생님 / 상태 / 검색어를 조합해 필요한 반을 찾으세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Select
            label="선생님"
            value={teacherId}
            onChange={(event) => {
              setTeacherId(event.target.value);
              setPage(0);
            }}
            className="lg:w-56"
            disabled={loading || teacherOptions.length === 0}
          >
            <option value="">전체</option>
            {teacherOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
          <Select
            label="상태"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as CourseStatusFilter);
              setPage(0);
            }}
            className="lg:w-40"
            disabled={loading}
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
          <Field label="검색어" className="lg:flex-1">
            <Input
              placeholder="반 이름 검색"
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
                setPage(0);
              }}
              disabled={loading}
            />
          </Field>
          <div className="flex items-end gap-3">
            <Button variant="secondary" onClick={resetFilters} disabled={loading}>
              초기화
            </Button>
          </div>
        </div>
        {error && <InlineError className="mt-4" message={error} />}
      </Card>

      <Card title="배정된 반" description="카드를 눌러 반 정보를 확인하세요.">
        {(!courses.length && !loading) ? (
          <EmptyState message="표시할 반이 없습니다." description={emptyDescription} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {courses.map((course) => (
              <article
                key={course.courseId}
                className="rounded-2xl border border-slate-200 p-5 transition hover:border-indigo-200 hover:bg-indigo-50/30"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      {course.companyName ?? "학원"} {course.branchName ?? ""}
                    </p>
                    <h3 className="mt-1 text-lg font-bold text-slate-900">{course.name}</h3>
                  </div>
                  <Badge variant={course.active ? "success" : "secondary"}>
                    {course.active ? "활성" : "비활성"}
                  </Badge>
                </div>
                {course.description && (
                  <p className="mt-2 text-sm text-slate-600 line-clamp-2">{course.description}</p>
                )}
                <dl className="mt-4 space-y-2 text-sm text-slate-600">
                  <div className="flex items-center gap-2">
                    <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">선생님</dt>
                    <dd>{course.teacherName ?? "-"}</dd>
                  </div>
                  <div className="flex items-center gap-2">
                    <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">기간</dt>
                    <dd>{formatPeriod(course)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">스케줄</dt>
                    <dd className="mt-1 text-slate-500">{scheduleSummary(course)}</dd>
                  </div>
                </dl>
              </article>
            ))}
          </div>
        )}
        {loading && <p className="mt-4 text-center text-sm text-slate-500">불러오는 중...</p>}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>
    </div>
  );
}

type PaginationProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

function Pagination({ currentPage, totalPages, onPageChange, disabled }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="mt-6 flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/70 px-4 py-3 text-sm text-slate-600">
      <span>
        페이지 {currentPage + 1} / {totalPages}
      </span>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          className="h-10 px-4 text-xs"
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={disabled || currentPage === 0}
        >
          이전
        </Button>
        <Button
          variant="secondary"
          className="h-10 px-4 text-xs"
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={disabled || currentPage >= totalPages - 1}
        >
          다음
        </Button>
      </div>
    </div>
  );
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
  return map[day] ?? day.slice(0, 3);
}

type FieldProps = {
  label: string;
  className?: string;
  children: ReactNode;
};

function Field({ label, className, children }: FieldProps) {
  return (
    <label className={clsx("flex w-full flex-col gap-1 text-sm font-semibold text-slate-700", className)}>
      <span>{label}</span>
      {children}
    </label>
  );
}
