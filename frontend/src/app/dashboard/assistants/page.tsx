"use client";

import { FormEvent, useMemo, useState } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/ui/error-state";
import { useAssistantList, useToggleAssistantActive } from "@/hooks/use-assistants";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type MemberSummary = components["schemas"]["MemberSummary"];
type ActiveFilter = "all" | "active" | "inactive";

const PAGE_SIZE = 20;

export default function AssistantsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("all");
  const [nameInput, setNameInput] = useState("");
  const [submittedName, setSubmittedName] = useState("");
  const [page, setPage] = useState(0);

  const activeValue = useMemo(() => {
    if (activeFilter === "all") return undefined;
    return activeFilter === "active";
  }, [activeFilter]);

  const assistantsQuery = useAssistantList({
    active: activeValue,
    name: submittedName || undefined,
    page
  });
  const isRefreshing = assistantsQuery.isFetching && !assistantsQuery.isLoading;

  const toggleAssistant = useToggleAssistantActive();

  if (!canRender) {
    return fallback;
  }

  const assistants = assistantsQuery.data?.content ?? [];
  const totalPages = assistantsQuery.data?.totalPages ?? 0;
  const currentPage = assistantsQuery.data?.page ?? page;
  const errorMessage = assistantsQuery.isError
    ? getApiErrorMessage(assistantsQuery.error, "조교 목록을 불러오지 못했습니다.")
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

  const handleToggle = (assistant: MemberSummary) => {
    if (!assistant.memberId) return;
    const name = assistant.name ?? "이름 없음";
    const isActive = assistant.active !== false;
    toggleAssistant.mutate({ memberId: assistant.memberId, name, active: isActive });
  };

  return (
    <DashboardShell title="조교 관리" subtitle="소속 조교를 조회하고 활성/비활성 상태를 전환할 수 있습니다.">
      <div className="space-y-6">
        <FilterBar
          activeFilter={activeFilter}
          nameInput={nameInput}
          isFetching={isRefreshing}
          onActiveChange={handleActiveChange}
          onNameChange={setNameInput}
          onSubmit={handleSearch}
        />

        {assistantsQuery.isLoading ? (
          <AssistantSkeleton />
        ) : assistantsQuery.isError ? (
          <ErrorState
            title="목록을 불러오지 못했습니다"
            description={errorMessage}
            retryLabel="다시 시도"
            onRetry={() => assistantsQuery.refetch()}
          />
        ) : assistants.length === 0 ? (
          <EmptyState />
        ) : (
          <AssistantList
            assistants={assistants}
            currentPage={currentPage}
            totalPages={totalPages}
            isFetching={isRefreshing}
            onToggle={handleToggle}
            onPageChange={handlePageChange}
            isMutating={toggleAssistant.isPending}
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

function FilterBar({
  activeFilter,
  nameInput,
  isFetching,
  onActiveChange,
  onNameChange,
  onSubmit
}: FilterBarProps) {
  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:flex-row md:items-end md:justify-between">
      <div className="flex flex-1 flex-col gap-3 md:flex-row md:items-end">
        <div className="flex items-center gap-2">
          <FilterChip active={activeFilter === "all"} label="전체" onClick={() => onActiveChange("all")} />
          <FilterChip
            active={activeFilter === "active"}
            label="활성"
            onClick={() => onActiveChange("active")}
          />
          <FilterChip
            active={activeFilter === "inactive"}
            label="비활성"
            onClick={() => onActiveChange("inactive")}
          />
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

type AssistantListProps = {
  assistants: MemberSummary[];
  currentPage: number;
  totalPages: number;
  isFetching: boolean;
  onToggle: (assistant: MemberSummary) => void;
  onPageChange: (page: number) => void;
  isMutating: boolean;
};

function AssistantList({
  assistants,
  currentPage,
  totalPages,
  isFetching,
  onToggle,
  onPageChange,
  isMutating
}: AssistantListProps) {
  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="hidden w-full table-fixed divide-y divide-slate-200 md:table">
          <thead className="bg-slate-50">
            <tr className="text-left text-sm font-semibold text-slate-600">
              <th className="px-4 py-3">이름</th>
              <th className="px-4 py-3">이메일</th>
              <th className="px-4 py-3 w-24">상태</th>
              <th className="px-4 py-3 w-32">생성일</th>
              <th className="px-4 py-3 w-32 text-right">액션</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {assistants.map((assistant) => (
              <tr key={assistant.memberId ?? assistant.email} className="text-sm text-slate-700">
                <td className="px-4 py-3 font-semibold text-slate-900">{assistant.name ?? "-"}</td>
                <td className="px-4 py-3">{assistant.email ?? "-"}</td>
                <td className="px-4 py-3">
                  <StatusBadge active={assistant.active} />
                </td>
                <td className="px-4 py-3 text-slate-500">{formatDate(assistant.createdAt)}</td>
                <td className="px-4 py-3 text-right">
                  <Button
                    variant={assistant.active === false ? "primary" : "secondary"}
                    className="h-10 px-4 text-sm"
                    disabled={isMutating}
                    onClick={() => onToggle(assistant)}
                  >
                    {assistant.active === false ? "활성화" : "비활성화"}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="space-y-3 p-4 md:hidden">
          {assistants.map((assistant) => (
            <div
              key={assistant.memberId ?? assistant.email}
              className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-base font-semibold text-slate-900">{assistant.name ?? "-"}</p>
                  <p className="text-sm text-slate-600">{assistant.email ?? "-"}</p>
                </div>
                <StatusBadge active={assistant.active} />
              </div>

              <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
                <span>생성일 {formatDate(assistant.createdAt)}</span>
                <Button
                  variant={assistant.active === false ? "primary" : "secondary"}
                  className="h-9 px-3 text-xs"
                  disabled={isMutating}
                  onClick={() => onToggle(assistant)}
                >
                  {assistant.active === false ? "활성화" : "비활성화"}
                </Button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={onPageChange} />

      {isFetching ? <p className="text-xs text-slate-500">최신 데이터로 동기화 중...</p> : null}
    </div>
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

function AssistantSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="hidden w-full divide-y divide-slate-200 md:table">
        <div className="bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-600">조교 목록을 불러오는 중...</div>
        <div className="divide-y divide-slate-100">
          {Array.from({ length: 5 }).map((_, index) => (
            <div key={index} className="flex items-center gap-4 px-4 py-4">
              <div className="h-4 w-32 animate-pulse rounded bg-slate-200" />
              <div className="h-4 w-48 animate-pulse rounded bg-slate-200" />
              <div className="h-4 w-20 animate-pulse rounded bg-slate-200" />
              <div className="h-10 w-24 animate-pulse rounded bg-slate-200" />
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
      <p className="text-lg font-semibold text-slate-900">등록된 조교가 없습니다.</p>
      <p className="mt-2 text-sm text-slate-600">초대 코드를 생성해 조교를 초대하세요.</p>
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

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" }).format(date);
}
