"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/ui/modal";
import type { EnrollmentStatus, TeacherEnrollmentRequestResponse } from "@/types/dashboard";
import { DASHBOARD_PAGE_SIZE, fetchAdminEnrollmentRequests } from "@/lib/dashboard-api";

const statusOptions: { label: string; value: EnrollmentStatus }[] = [
  { label: "대기", value: "PENDING" },
  { label: "승인", value: "APPROVED" },
  { label: "거절", value: "REJECTED" },
  { label: "취소", value: "CANCELED" }
];

const statusBadgeVariant: Record<EnrollmentStatus, Parameters<typeof Badge>[0]["variant"]> = {
  PENDING: "secondary",
  APPROVED: "success",
  REJECTED: "destructive",
  CANCELED: "secondary"
};

export default function AdminStudentEnrollmentPage() {
  const { canRender, fallback } = useRoleGuard("SUPER_ADMIN");
  if (!canRender) {
    return fallback;
  }
  return <AdminStudentEnrollmentContent />;
}

function AdminStudentEnrollmentContent() {
  const [teacherIdInput, setTeacherIdInput] = useState("");
  const [courseIdInput, setCourseIdInput] = useState("");
  const [studentNameInput, setStudentNameInput] = useState("");
  const [selectedStatuses, setSelectedStatuses] = useState<EnrollmentStatus[]>([]);
  const [page, setPage] = useState(0);

  const teacherId = useDebounce(teacherIdInput.trim(), 400);
  const courseId = useDebounce(courseIdInput.trim(), 400);
  const studentName = useDebounce(studentNameInput.trim(), 400);

  const [requests, setRequests] = useState<TeacherEnrollmentRequestResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [detail, setDetail] = useState<TeacherEnrollmentRequestResponse | null>(null);

  const loadRequests = useCallback(
    async (targetPage: number) => {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchAdminEnrollmentRequests({
          teacherId,
          courseId,
          statuses: selectedStatuses.length ? selectedStatuses : undefined,
          studentName,
          page: targetPage,
          size: DASHBOARD_PAGE_SIZE
        });
        setRequests(result.items);
        setTotal(result.totalElements);
      } catch (err) {
        const message = err instanceof Error ? err.message : "학생 신청 목록을 불러오지 못했습니다.";
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    [teacherId, courseId, selectedStatuses, studentName]
  );

  useEffect(() => {
    void loadRequests(page);
  }, [page, loadRequests]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);
  const toolbarDisabled = loading;

  const statusSummary = useMemo(() => {
    if (selectedStatuses.length === 0) {
      return "전체";
    }
    return selectedStatuses.map((value) => statusOptions.find((option) => option.value === value)?.label ?? value).join(", ");
  }, [selectedStatuses]);

const handleStatusToggle = (status: EnrollmentStatus, checked: boolean) => {
    setSelectedStatuses((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(status);
      } else {
        next.delete(status);
      }
      return Array.from(next);
    });
    setPage(0);
  };

  const handleResetFilters = () => {
    setTeacherIdInput("");
    setCourseIdInput("");
    setStudentNameInput("");
    setSelectedStatuses([]);
    setPage(0);
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-rose-500">Admin · Enrollment</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">학생 요청 관리</h1>
        <p className="mt-2 text-sm text-slate-500">
          Teacher/반/상태 기준으로 전체 학생 신청을 감사하고, 개별 신청의 상세 내용을 확인하세요.
        </p>
      </header>

      <Card title="검색 필터" description="Teacher/Course UUID와 상태를 조합해 원하는 신청만 추려보세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Field label="Teacher ID" className="lg:w-60">
            <Input
              placeholder="Teacher UUID"
              value={teacherIdInput}
              onChange={(event) => {
                setTeacherIdInput(event.target.value);
                setPage(0);
              }}
              disabled={toolbarDisabled}
            />
          </Field>
          <Field label="Course ID" className="lg:w-60">
            <Input
              placeholder="Course UUID"
              value={courseIdInput}
              onChange={(event) => {
                setCourseIdInput(event.target.value);
                setPage(0);
              }}
              disabled={toolbarDisabled}
            />
          </Field>
          <Field label="학생 이름" className="lg:w-60">
            <Input
              placeholder="검색어"
              value={studentNameInput}
              onChange={(event) => {
                setStudentNameInput(event.target.value);
                setPage(0);
              }}
              disabled={toolbarDisabled}
            />
          </Field>
        </div>

        <div className="mt-5 rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-4">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold text-slate-700">상태 필터</p>
              <p className="text-xs text-slate-500">현재 선택: {statusSummary}</p>
            </div>
            <Button variant="secondary" onClick={() => setSelectedStatuses([])} disabled={toolbarDisabled}>
              전체 선택 해제
            </Button>
          </div>
          <div className="mt-3 flex flex-wrap gap-4">
            {statusOptions.map((option) => {
              const checked = selectedStatuses.includes(option.value);
              return (
                <Checkbox
                  key={option.value}
                  label={option.label}
                  checked={checked}
                  onChange={(event) => handleStatusToggle(option.value, event.target.checked)}
                  disabled={toolbarDisabled}
                />
              );
            })}
          </div>
        </div>

        <div className="mt-4 flex flex-wrap gap-3">
          <Button variant="secondary" onClick={handleResetFilters} disabled={toolbarDisabled}>
            필터 초기화
          </Button>
          <Button onClick={() => void loadRequests(page)} disabled={toolbarDisabled}>
            새로고침
          </Button>
        </div>
      </Card>

      <Card
        title="학생 신청 목록"
        description="테이블을 클릭하면 Course·학생·처리 이력을 포함한 상세 정보가 열립니다."
      >
        {error && <InlineError message={error} className="mb-4" />}
        {loading && (
          <div className="py-16 text-center text-sm text-slate-500">
            학생 신청을 불러오는 중입니다. 잠시만 기다려 주세요.
          </div>
        )}
        {!loading && requests.length === 0 && (
          <EmptyState message="표시할 신청이 없습니다." description="필터를 조정하거나 새로고침해 보세요." />
        )}
        {!loading && requests.length > 0 && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>신청일</TableHead>
                  <TableHead>학생</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>상태</TableHead>
                  <TableHead>메시지</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {requests.map((request, index) => {
                  const rowKey =
                    request.requestId ??
                    `${request.student?.memberId ?? "student"}-${request.course?.courseId ?? "course"}-${request.createdAt ?? index}`;
                  return (
                  <TableRow
                    key={rowKey}
                    className="cursor-pointer transition hover:bg-slate-50"
                    onClick={() => setDetail(request)}
                  >
                    <TableCell className="text-sm text-slate-600">{formatDateTime(request.createdAt)}</TableCell>
                    <TableCell>
                      <div className="flex flex-col text-sm">
                        <span className="font-semibold text-slate-900">{request.student?.name ?? "-"}</span>
                        <span className="text-xs text-slate-500">{request.student?.email ?? request.student?.phoneNumber}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-col text-sm">
                        <span className="font-semibold text-slate-900">{request.course?.name ?? "-"}</span>
                        <span className="text-xs text-slate-500">{request.course?.branchName}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusBadgeVariant[request.status ?? "PENDING"]}>
                        {statusOptions.find((option) => option.value === request.status)?.label ?? request.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="max-w-xs text-sm text-slate-600">{request.studentMessage ?? "-"}</TableCell>
                  </TableRow>
                )})}
              </TableBody>
            </Table>
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <DetailModal open={Boolean(detail)} request={detail} onClose={() => setDetail(null)} />
    </div>
  );
}

type DetailModalProps = {
  open: boolean;
  request: TeacherEnrollmentRequestResponse | null;
  onClose: () => void;
};

function DetailModal({ open, request, onClose }: DetailModalProps) {
  if (!request) {
    return null;
  }
  const statusLabel = statusOptions.find((option) => option.value === request.status)?.label ?? request.status;
  return (
    <Modal open={open} onClose={onClose} title="신청 상세 정보" size="lg">
      <div className="space-y-6">
        <section>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">상태</p>
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <Badge variant={statusBadgeVariant[request.status ?? "PENDING"]}>{statusLabel}</Badge>
            <span className="text-sm text-slate-500">
              신청일 {formatDateTime(request.createdAt)} / 처리일 {formatDateTime(request.processedAt)}
            </span>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2">
          <InfoCard
            title="학생 정보"
            items={[
              { label: "이름", value: request.student?.name ?? "-" },
              { label: "이메일", value: request.student?.email ?? "-" },
              { label: "연락처", value: request.student?.phoneNumber ?? "-" },
              { label: "학교/학년", value: `${request.student?.schoolName ?? "-"} ${request.student?.grade ?? ""}`.trim() },
            ]}
          />
          <InfoCard
            title="Course 정보"
            items={[
              { label: "반 이름", value: request.course?.name ?? "-" },
              { label: "지점", value: request.course?.branchName ?? "-" },
              { label: "회사", value: request.course?.companyName ?? "-" },
              { label: "기간", value: formatCoursePeriod(request.course?.startDate, request.course?.endDate) }
            ]}
          />
        </section>

        <section className="rounded-2xl border border-slate-200/80 bg-slate-50 px-4 py-4">
          <p className="text-sm font-semibold text-slate-800">학생 메시지</p>
          <p className="mt-2 text-sm text-slate-600">{request.studentMessage ?? "남긴 메시지가 없습니다."}</p>
        </section>

        <section className="rounded-2xl border border-slate-200/80 bg-slate-50 px-4 py-4">
          <p className="text-sm font-semibold text-slate-800">처리 정보</p>
          <p className="mt-2 text-sm text-slate-600">
            처리자 ID: {request.processedByMemberId ?? "-"} / 처리일: {formatDateTime(request.processedAt)}
          </p>
        </section>

        <div className="flex justify-end">
          <Button onClick={onClose}>닫기</Button>
        </div>
      </div>
    </Modal>
  );
}

type InfoCardProps = {
  title: string;
  items: { label: string; value: ReactNode }[];
};

function InfoCard({ title, items }: InfoCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200/70 bg-white px-4 py-4 shadow-sm">
      <p className="text-sm font-semibold text-slate-800">{title}</p>
      <dl className="mt-3 space-y-2 text-sm text-slate-600">
        {items.map((item) => (
          <div key={item.label} className="flex justify-between gap-4">
            <dt className="w-24 text-xs font-semibold uppercase tracking-wide text-slate-500">{item.label}</dt>
            <dd className="flex-1 text-right">{item.value}</dd>
          </div>
        ))}
      </dl>
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
    <div className="mt-6 flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/60 px-4 py-3">
      <p className="text-xs font-medium text-slate-500">
        페이지 {currentPage + 1} / {totalPages}
      </p>
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

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  try {
    return new Intl.DateTimeFormat("ko", {
      dateStyle: "medium",
      timeStyle: "short"
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function formatCoursePeriod(start?: string | null, end?: string | null) {
  if (!start && !end) {
    return "-";
  }
  const formatDate = (value?: string | null) => {
    if (!value) return "?";
    try {
      return new Intl.DateTimeFormat("ko", { dateStyle: "medium" }).format(new Date(value));
    } catch {
      return value;
    }
  };
  return `${formatDate(start)} ~ ${formatDate(end)}`;
}
