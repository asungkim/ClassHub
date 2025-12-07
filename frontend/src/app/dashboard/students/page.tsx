"use client";

import { FormEvent, useMemo, useState } from "react";
import clsx from "clsx";
import Link from "next/link";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/ui/error-state";
import { useStudentProfileList } from "@/hooks/use-student-profiles";
import { getApiErrorMessage } from "@/lib/api-error";
import { useSession } from "@/components/session/session-provider";
import type { components } from "@/types/openapi";

type StudentProfileSummary = components["schemas"]["StudentProfileSummary"];
type ActiveFilter = "all" | "active" | "inactive";

const PAGE_SIZE = 20;

export default function StudentsPage() {
  const { canRender, fallback } = useRoleGuard(["TEACHER", "ASSISTANT"]);
  const { member } = useSession();
  const isTeacher = member?.role === "TEACHER";
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("all");
  const [nameInput, setNameInput] = useState("");
  const [submittedName, setSubmittedName] = useState("");
  const [page, setPage] = useState(0);

  const activeValue = useMemo(() => {
    if (activeFilter === "all") return undefined;
    return activeFilter === "active";
  }, [activeFilter]);

  const studentsQuery = useStudentProfileList({
    active: activeValue,
    name: submittedName || undefined,
    page
  });
  const isRefreshing = studentsQuery.isFetching && !studentsQuery.isLoading;

  if (!canRender) {
    return fallback;
  }

  const students = studentsQuery.data?.content ?? [];
  const totalPages = studentsQuery.data?.totalPages ?? 0;
  const currentPage = studentsQuery.data?.page ?? page;
  const errorMessage = studentsQuery.isError
    ? getApiErrorMessage(studentsQuery.error, "학생 목록을 불러오지 못했습니다.")
    : "";

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmittedName(nameInput.trim());
    setPage(0);
  };

  const handleActiveChange = (value: ActiveFilter) => {
    setActiveFilter(value);
    setPage(0);
  };

  const handlePageChange = (nextPage: number) => {
    setPage(nextPage);
  };

  return (
    <DashboardShell title="학생 관리" subtitle="학생 목록을 조회하고 상태를 확인할 수 있습니다.">
      <div className="space-y-6">
        <FilterBar
          activeFilter={activeFilter}
          nameInput={nameInput}
          isFetching={isRefreshing}
          onActiveChange={handleActiveChange}
          onNameChange={setNameInput}
          onSubmit={handleSearch}
        />

        {isTeacher ? (
          <div className="flex justify-end">
            <Button asChild className="h-11 px-4 text-sm">
              <Link href="/dashboard/students/new">학생 등록</Link>
            </Button>
          </div>
        ) : null}

        {studentsQuery.isLoading ? (
          <StudentSkeleton />
        ) : studentsQuery.isError ? (
          <ErrorState
            title="목록을 불러오지 못했습니다"
            description={errorMessage}
            retryLabel="다시 시도"
            onRetry={() => studentsQuery.refetch()}
          />
        ) : students.length === 0 ? (
          <EmptyState />
        ) : (
          <StudentList
            students={students}
            currentPage={currentPage}
            totalPages={totalPages}
            isFetching={isRefreshing}
            onPageChange={handlePageChange}
            isTeacher={isTeacher}
          />
        )}
      </div>
    </DashboardShell>
  );
}

type FilterBarProps = {
  activeFilter: ActiveFilter;
  nameInput: string;
  isFetching: boolean;
  onActiveChange: (value: ActiveFilter) => void;
  onNameChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

function FilterBar({ activeFilter, nameInput, isFetching, onActiveChange, onNameChange, onSubmit }: FilterBarProps) {
  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:flex-row md:items-end md:justify-between">
      <div className="flex flex-1 flex-col gap-3 md:flex-row md:items-end">
        <div className="flex items-center gap-2">
          <FilterChip active={activeFilter === "all"} label="전체" onClick={() => onActiveChange("all")} />
          <FilterChip active={activeFilter === "active"} label="활성" onClick={() => onActiveChange("active")} />
          <FilterChip active={activeFilter === "inactive"} label="비활성" onClick={() => onActiveChange("inactive")} />
        </div>

        <form className="flex flex-1 flex-col gap-2 md:flex-row md:items-end" onSubmit={onSubmit}>
          <div className="w-full md:max-w-md">
            <TextField
              label="이름 검색"
              placeholder="이름을 입력하세요"
              value={nameInput}
              onChange={(e) => onNameChange(e.target.value)}
            />
          </div>
          <Button type="submit" className="h-12 md:w-28">
            검색
          </Button>
        </form>
      </div>

      {isFetching ? (
        <p className="text-xs font-semibold text-blue-600">목록을 새로고침 중...</p>
      ) : (
        <p className="text-xs text-slate-500">페이지당 {PAGE_SIZE}명</p>
      )}
    </div>
  );
}

type StudentListProps = {
  students: StudentProfileSummary[];
  currentPage: number;
  totalPages: number;
  isFetching: boolean;
  onPageChange: (page: number) => void;
  isTeacher: boolean;
};

function StudentList({ students, currentPage, totalPages, isFetching, onPageChange, isTeacher }: StudentListProps) {
  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="hidden w-full table-fixed divide-y divide-slate-200 md:table">
          <thead className="bg-slate-50">
            <tr className="text-left text-sm font-semibold text-slate-600">
              <th className="px-4 py-3">이름</th>
              <th className="px-4 py-3">전화번호</th>
              <th className="px-4 py-3 w-24">상태</th>
              <th className="px-4 py-3 w-20">나이</th>
              <th className="px-4 py-3 w-32">생성일</th>
              {isTeacher ? <th className="px-4 py-3 w-32 text-right">액션</th> : null}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {students.map((student) => (
              <tr key={student.id ?? student.memberId} className="text-sm text-slate-700">
                <td className="px-4 py-3 font-semibold text-slate-900">{student.name ?? "-"}</td>
                <td className="px-4 py-3">{student.phoneNumber ?? "-"}</td>
                <td className="px-4 py-3">
                  <StatusBadge active={student.active} />
                </td>
                <td className="px-4 py-3">{formatAge(student.age)}</td>
                <td className="px-4 py-3 text-slate-500">{formatDate(undefined)}</td>
                {isTeacher ? (
                  <td className="px-4 py-3 text-right">
                    <Button variant="secondary" className="h-10 px-4 text-sm" asChild>
                      <Link href={`/dashboard/students/${student.id}/edit`}>수정</Link>
                    </Button>
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
        </table>

        <div className="space-y-3 p-4 md:hidden">
          {students.map((student) => (
            <div
              key={student.id ?? student.memberId}
              className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-base font-semibold text-slate-900">{student.name ?? "-"}</p>
                  <p className="text-sm text-slate-600">{student.phoneNumber ?? "-"}</p>
                </div>
                <StatusBadge active={student.active} />
              </div>

              <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
                <span>나이 {formatAge(student.age)}</span>
                <span>{formatDate(undefined)}</span>
              </div>
              {isTeacher ? (
                <div className="mt-3 flex justify-end">
                  <Button variant="secondary" className="h-9 px-3 text-xs" asChild>
                    <Link href={`/dashboard/students/${student.id}/edit`}>수정</Link>
                  </Button>
                </div>
              ) : null}
            </div>
          ))}
        </div>
      </div>

      <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={onPageChange} />

      {isFetching ? <p className="text-xs text-slate-500">최신 데이터로 동기화 중...</p> : null}
    </div>
  );
}

function FilterChip({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        "rounded-xl border px-3 py-2 text-sm font-semibold transition",
        active
          ? "border-blue-200 bg-blue-50 text-blue-700 shadow-sm"
          : "border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:text-blue-700"
      )}
    >
      {label}
    </button>
  );
}

function StatusBadge({ active }: { active?: boolean }) {
  return (
    <span
      className={clsx(
        "inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold",
        active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-600"
      )}
    >
      <span
        className={clsx(
          "h-2 w-2 rounded-full",
          active ? "bg-emerald-500 shadow-[0_0_0_4px_rgba(16,185,129,0.15)]" : "bg-slate-400"
        )}
      />
      {active ? "활성" : "비활성"}
    </span>
  );
}

function StudentSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="hidden w-full divide-y divide-slate-200 md:table">
        <div className="bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-600">학생 목록을 불러오는 중...</div>
        <div className="divide-y divide-slate-100">
          {Array.from({ length: 5 }).map((_, index) => (
            <div key={index} className="flex items-center gap-4 px-4 py-4">
              <div className="h-4 w-32 animate-pulse rounded bg-slate-200" />
              <div className="h-4 w-48 animate-pulse rounded bg-slate-200" />
              <div className="h-4 w-20 animate-pulse rounded bg-slate-200" />
              <div className="h-4 w-16 animate-pulse rounded bg-slate-200" />
            </div>
          ))}
        </div>
      </div>

      <div className="space-y-3 p-4 md:hidden">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="rounded-xl border border-slate-200 p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div className="h-5 w-32 animate-pulse rounded bg-slate-200" />
              <div className="h-6 w-16 animate-pulse rounded bg-slate-200" />
            </div>
            <div className="mt-3 h-3 w-40 animate-pulse rounded bg-slate-200" />
          </div>
        ))}
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center shadow-sm">
      <p className="text-lg font-semibold text-slate-900">등록된 학생이 없습니다.</p>
      <p className="mt-2 text-sm text-slate-600">학생을 등록하거나 초대하세요.</p>
    </div>
  );
}

function Pagination({
  currentPage,
  totalPages,
  onPageChange
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (!totalPages || totalPages <= 1) {
    return null;
  }

  const pages = Array.from({ length: totalPages }, (_, index) => index);

  return (
    <div className="flex flex-wrap items-center justify-center gap-2">
      <Button
        variant="secondary"
        className="h-10 px-3 text-sm"
        disabled={currentPage <= 0}
        onClick={() => onPageChange(currentPage - 1)}
      >
        이전
      </Button>
      {pages.map((page) => (
        <button
          key={page}
          type="button"
          onClick={() => onPageChange(page)}
          className={clsx(
            "h-10 min-w-[2.5rem] rounded-lg border px-3 text-sm font-semibold transition",
            page === currentPage
              ? "border-blue-200 bg-blue-50 text-blue-700 shadow-sm"
              : "border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:text-blue-700"
          )}
        >
          {page + 1}
        </button>
      ))}
      <Button
        variant="secondary"
        className="h-10 px-3 text-sm"
        disabled={currentPage >= totalPages - 1}
        onClick={() => onPageChange(currentPage + 1)}
      >
        다음
      </Button>
    </div>
  );
}

function formatAge(age?: number | null) {
  if (age === null || age === undefined) return "-";
  return `${age}세`;
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" }).format(date);
}
