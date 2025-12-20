"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/components/ui/toast";
import type { PublicCourseResponse } from "@/types/dashboard";
import { DASHBOARD_PAGE_SIZE, fetchPublicCourses } from "@/lib/dashboard-api";

type Option = { value: string; label: string };

export default function StudentCourseSearchPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return <StudentCourseSearchContent />;
}

function StudentCourseSearchContent() {
  const { showToast } = useToast();
  const [companyId, setCompanyId] = useState("");
  const [branchId, setBranchId] = useState("");
  const [teacherId, setTeacherId] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [onlyVerified, setOnlyVerified] = useState(true);
  const keyword = useDebounce(keywordInput.trim(), 300);
  const [page, setPage] = useState(0);

  const [courses, setCourses] = useState<PublicCourseResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [companyOptions, setCompanyOptions] = useState<Option[]>([]);
  const [branchOptionMap, setBranchOptionMap] = useState<Record<string, Record<string, string>>>({});
  const [teacherOptions, setTeacherOptions] = useState<Option[]>([]);

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchPublicCourses({
        companyId: companyId || undefined,
        branchId: branchId || undefined,
        teacherId: teacherId || undefined,
        keyword: keyword || undefined,
        onlyVerified,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setCourses(result.items);
      setTotal(result.totalElements);
      setTeacherOptions((prev) => {
        const map = new Map(prev.map((opt) => [opt.value, opt.label] as const));
        result.items.forEach((course) => {
          if (course.teacherId) {
            map.set(course.teacherId, course.teacherName ?? "이름 없음");
          }
        });
        return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
      });
      setCompanyOptions((prev) => {
        const map = new Map(prev.map((opt) => [opt.value, opt.label] as const));
        result.items.forEach((course) => {
          if (course.companyId) {
            map.set(course.companyId, course.companyName ?? "학원");
          }
        });
        return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
      });
      setBranchOptionMap((prev) => {
        const next: Record<string, Record<string, string>> = { ...prev };
        result.items.forEach((course) => {
          if (course.companyId && course.branchId) {
            const existing = next[course.companyId] ? { ...next[course.companyId] } : {};
            existing[course.branchId] = course.branchName ?? "지점";
            next[course.companyId] = existing;
          }
        });
        return next;
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : "공개 반을 불러오지 못했습니다.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [branchId, companyId, keyword, onlyVerified, page, teacherId]);

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  const branchSelectOptions = useMemo(() => {
    if (companyId && branchOptionMap[companyId]) {
      return Object.entries(branchOptionMap[companyId]).map(([value, label]) => ({ value, label }));
    }
    const union = new Map<string, string>();
    Object.values(branchOptionMap).forEach((entries) => {
      Object.entries(entries).forEach(([value, label]) => union.set(value, label));
    });
    return Array.from(union.entries()).map(([value, label]) => ({ value, label }));
  }, [branchOptionMap, companyId]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const resetFilters = () => {
    setCompanyId("");
    setBranchId("");
    setTeacherId("");
    setKeywordInput("");
    setOnlyVerified(true);
    setPage(0);
  };

  const handleRequestClick = () => {
    showToast("info", "등록 요청 기능은 준비 중입니다.");
  };

  const emptyDescription = useMemo(() => {
    if (onlyVerified) {
      return "검증된 학원에서 운영 중인 반이 없습니다. 다른 필터를 선택해보세요.";
    }
    return "조건에 맞는 반이 없습니다. 검색어 또는 필터를 변경해 보세요.";
  }, [onlyVerified]);

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Student · Course Search</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">반 검색</h1>
        <p className="mt-2 text-sm text-slate-500">
          공개된 Course를 학원/지점/선생님 기준으로 찾아보고, 마음에 드는 반을 선택해 등록 요청 준비를 해두세요.{" "}
          승인된 내역은 ‘내 수업’ 페이지에서 확인하게 됩니다.
        </p>
      </header>

      <Card title="검색 필터" description="학원/지점/선생님/검색어를 조합해 원하는 반을 찾으세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Field label="학원(Company)" className="lg:w-60">
            <Select
              value={companyId}
              onChange={(event) => {
                setCompanyId(event.target.value);
                setBranchId("");
                setPage(0);
              }}
              disabled={loading}
            >
              <option value="">전체</option>
              {companyOptions.map((company) => (
                <option key={company.value} value={company.value}>
                  {company.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="지점(Branch)" className="lg:w-60">
            <Select
              value={branchId}
              onChange={(event) => {
                setBranchId(event.target.value);
                setPage(0);
              }}
              disabled={loading || branchSelectOptions.length === 0}
            >
              <option value="">전체</option>
              {branchSelectOptions.map((branch) => (
                <option key={branch.value} value={branch.value}>
                  {branch.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="선생님" className="lg:w-56">
            <Select
              value={teacherId}
              onChange={(event) => {
                setTeacherId(event.target.value);
                setPage(0);
              }}
              disabled={loading || teacherOptions.length === 0}
            >
              <option value="">전체</option>
              {teacherOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="검색어" className="flex-1">
            <Input
              placeholder="반 이름을 입력하세요."
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
                setPage(0);
              }}
              disabled={loading}
            />
          </Field>
          <div className="flex items-end gap-4">
            <Checkbox
              checked={onlyVerified}
              onChange={(event) => {
                setOnlyVerified(event.target.checked);
                setPage(0);
              }}
              label="검증된 학원만 보기"
            />
            <Button variant="secondary" onClick={resetFilters} disabled={loading}>
              초기화
            </Button>
          </div>
        </div>
        {error && <InlineError className="mt-4" message={error} />}
      </Card>

      <Card title="공개 Course" description="선택한 조건에 맞는 Course 목록입니다.">
        {(!courses.length && !loading) ? (
          <div className="py-10">
            <EmptyState message="표시할 반이 없습니다." description={emptyDescription} />
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {courses.map((course) => (
              <article
                key={course.courseId}
                className="rounded-2xl border border-slate-200 p-5 transition hover:border-indigo-200 hover:bg-indigo-50/30"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">
                      {formatAcademyName(course)}
                    </p>
                    <h3 className="mt-1 text-lg font-bold text-slate-900">{course.name}</h3>
                    <p className="text-sm text-slate-500">{course.teacherName ?? "선생님 미지정"}</p>
                  </div>
                  <Badge variant={course.active ? "success" : "secondary"}>
                    {course.active ? "모집 중" : "비활성"}
                  </Badge>
                </div>
                {course.description && (
                  <p className="mt-2 text-sm text-slate-600 line-clamp-2">{course.description}</p>
                )}
                <dl className="mt-4 space-y-2 text-sm text-slate-600">
                  <div className="flex gap-2">
                    <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">기간</dt>
                    <dd>{formatPeriod(course)}</dd>
                  </div>
                  <div className="flex gap-2">
                    <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">스케줄</dt>
                    <dd className="text-slate-500">{courseScheduleSummary(course)}</dd>
                  </div>
                </dl>
                <Button className="mt-4 w-full" onClick={handleRequestClick}>
                  등록 요청
                </Button>
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
    <div className="mt-6 flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3 text-sm text-slate-600">
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

function formatPeriod(course: PublicCourseResponse) {
  if (!course.startDate || !course.endDate) {
    return "일정 미정";
  }
  return `${course.startDate} ~ ${course.endDate}`;
}

function formatAcademyName(course: PublicCourseResponse) {
  const company = course.companyName ?? "학원";
  const branch = course.branchName ?? "";
  return `${company} ${branch}`.trim();
}

function courseScheduleSummary(course: PublicCourseResponse) {
  if (course.scheduleSummary && course.scheduleSummary.length > 0) {
    return course.scheduleSummary;
  }
  if (!course.schedules || course.schedules.length === 0) {
    return "시간 정보 없음";
  }
  return course.schedules
    .map((schedule) => `${weekdayLabel(schedule.dayOfWeek)} ${schedule.startTime}~${schedule.endTime}`)
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
