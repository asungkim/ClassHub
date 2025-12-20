"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/shared/empty-state";
import { InlineError } from "@/components/ui/inline-error";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import type {
  EnrollmentStatus,
  StudentCourseResponse,
  StudentEnrollmentRequestResponse
} from "@/types/dashboard";
import {
  DASHBOARD_PAGE_SIZE,
  cancelStudentEnrollmentRequest,
  fetchMyEnrollmentRequests,
  fetchStudentMyCourses
} from "@/lib/dashboard-api";

type TabKey = "courses" | "requests";

const enrollmentStatusOptions: { value: EnrollmentStatus; label: string }[] = [
  { value: "PENDING", label: "대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "거절" },
  { value: "CANCELED", label: "취소" }
];

const statusBadgeVariant: Record<EnrollmentStatus, Parameters<typeof Badge>[0]["variant"]> = {
  PENDING: "secondary",
  APPROVED: "success",
  REJECTED: "destructive",
  CANCELED: "secondary"
};

export default function StudentMyCoursesPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return <StudentMyCoursesContent />;
}

function StudentMyCoursesContent() {
  const searchParams = useSearchParams();
  const initialTab = searchParams.get("tab") === "requests" ? "requests" : "courses";
  const [activeTab, setActiveTab] = useState<TabKey>(initialTab);

  const handleTabChange = (value: string) => {
    const nextTab: TabKey = value === "requests" ? "requests" : "courses";
    setActiveTab(nextTab);
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Student · My Courses</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">내 수업</h1>
        <p className="mt-2 text-sm text-slate-500">
          승인된 수업과 신청 내역을 한 번에 확인하고, 필요하면 신청을 취소할 수 있습니다. 아직 수업이 없다면 반 검색에서
          원하는 수업을 신청해 보세요.
        </p>
      </header>

      <Tabs defaultValue={activeTab} value={activeTab} onValueChange={handleTabChange} className="space-y-4">
        <TabsList>
          <TabsTrigger value="courses">수업 목록</TabsTrigger>
          <TabsTrigger value="requests">신청 내역</TabsTrigger>
        </TabsList>

        <TabsContent value="courses">
          <CoursesTab />
        </TabsContent>
        <TabsContent value="requests">
          <RequestsTab />
        </TabsContent>
      </Tabs>
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
              disabled={loading}
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
              description="반 검색 페이지에서 원하는 수업을 찾아 신청하면 승인 후 이곳에 표시됩니다."
            />
            <div className="mt-6 text-center">
              <Button asChild>
                <Link href="/student/course/search">반 검색하러 가기</Link>
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

function RequestsTab() {
  const { showToast } = useToast();
  const [statuses, setStatuses] = useState<EnrollmentStatus[]>(["PENDING"]);
  const [page, setPage] = useState(0);
  const [requests, setRequests] = useState<StudentEnrollmentRequestResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<StudentEnrollmentRequestResponse | null>(null);
  const [cancelLoading, setCancelLoading] = useState(false);

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchMyEnrollmentRequests({
        statuses: statuses.length > 0 ? statuses : undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setRequests(result.items);
      setTotal(result.totalElements);
    } catch (err) {
      const message = err instanceof Error ? err.message : "신청 내역을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [page, showToast, statuses]);

  useEffect(() => {
    void loadRequests();
  }, [loadRequests]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const toggleStatus = (value: EnrollmentStatus, checked: boolean) => {
    setStatuses((prev) => {
      const set = new Set(prev);
      if (checked) {
        set.add(value);
      } else {
        set.delete(value);
      }
      return Array.from(set);
    });
    setPage(0);
  };

  const cancelRequest = async () => {
    if (!cancelTarget?.requestId) return;
    setCancelLoading(true);
    try {
      await cancelStudentEnrollmentRequest(cancelTarget.requestId);
      showToast("success", "신청을 취소했습니다.");
      setCancelTarget(null);
      await loadRequests();
    } catch (err) {
      const message = err instanceof Error ? err.message : "신청을 취소하지 못했습니다.";
      showToast("error", message);
    } finally {
      setCancelLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card title="상태 필터" description="조회하고 싶은 상태를 선택하세요. 기본값은 대기 중인 신청입니다.">
        <div className="flex flex-wrap gap-4">
          {enrollmentStatusOptions.map((option) => (
            <label key={option.value} className="inline-flex items-center gap-2 text-sm text-slate-600">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-400"
                checked={statuses.includes(option.value)}
                onChange={(event) => toggleStatus(option.value, event.target.checked)}
                disabled={loading}
              />
              {option.label}
            </label>
          ))}
        </div>
        <div className="mt-4 flex gap-3">
          <Button variant="secondary" onClick={() => setStatuses(["PENDING"])} disabled={loading}>
            기본값으로
          </Button>
          <Button onClick={() => void loadRequests()} disabled={loading}>
            새로고침
          </Button>
        </div>
        {error && <InlineError className="mt-4" message={error} />}
      </Card>

      <Card title="신청 내역" description="진행 중/완료된 모든 신청을 확인할 수 있습니다.">
        {loading && <p className="py-12 text-center text-sm text-slate-500">신청 내역을 불러오는 중입니다...</p>}
        {!loading && requests.length === 0 && (
          <div className="py-12">
            <EmptyState
              message="등록된 신청이 없습니다."
              description="반 검색 페이지에서 신청을 보내면 여기에서 상태를 확인할 수 있습니다."
            />
            <div className="mt-6 text-center">
              <Button asChild>
                <Link href="/student/course/search">반 검색하러 가기</Link>
              </Button>
            </div>
          </div>
        )}
        {!loading && requests.length > 0 && (
          <div className="space-y-4">
            {requests.map((request, index) => {
              const key = request.requestId ?? `request-${index}`;
              const course = request.course;
              const status = request.status ?? "PENDING";
              const cancellable = status === "PENDING" && Boolean(request.requestId);
              return (
                <article key={key} className="rounded-2xl border border-slate-200 p-5 shadow-sm">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                        신청일 {formatDate(request.createdAt)}
                      </p>
                      <h3 className="mt-1 text-lg font-semibold text-slate-900">{course?.name ?? "이름 없는 반"}</h3>
                      <p className="text-sm text-slate-500">{formatAcademyName(course)}</p>
                    </div>
                    <Badge variant={statusBadgeVariant[status]}>{statusToLabel(status)}</Badge>
                  </div>
                  <dl className="mt-4 space-y-2 text-sm text-slate-600">
                    <div className="flex gap-2">
                      <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">기간</dt>
                      <dd>{formatPeriod(course?.startDate, course?.endDate)}</dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">메시지</dt>
                      <dd className="text-slate-500">{request.message ?? "남긴 메시지가 없습니다."}</dd>
                    </div>
                  </dl>
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
                    <span>
                      처리일 {formatDate(request.processedAt)} · 처리자 {request.processedByMemberId ?? "-"}
                    </span>
                    {cancellable && (
                      <Button
                        variant="ghost"
                        className="text-sm"
                        onClick={() => setCancelTarget(request)}
                        disabled={loading}
                      >
                        신청 취소
                      </Button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        onClose={() => setCancelTarget(null)}
        onConfirm={cancelRequest}
        isLoading={cancelLoading}
        title="신청 취소"
        message="해당 신청을 취소하면 다시 등록 요청을 보내야 합니다. 취소하시겠습니까?"
        confirmText="취소하기"
        cancelText="닫기"
      />
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

function statusToLabel(status: EnrollmentStatus) {
  return enrollmentStatusOptions.find((option) => option.value === status)?.label ?? status;
}
