"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import type { components } from "@/types/openapi";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { useToast } from "@/components/ui/toast";
import { Badge } from "@/components/ui/badge";
import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPhoneNumber } from "@/lib/format-phone";
import { Modal } from "@/components/ui/modal";
import { Input } from "@/components/ui/input";

const assistantTabs = [
  { label: "활성", value: "ACTIVE" },
  { label: "비활성", value: "INACTIVE" },
  { label: "전체", value: "ALL" }
] as const;

type AssistantFilter = (typeof assistantTabs)[number]["value"];

type AssistantAssignmentResponse = components["schemas"]["AssistantAssignmentResponse"];
type AssistantAssignmentStatusUpdateRequest = components["schemas"]["AssistantAssignmentStatusUpdateRequest"];
type AssistantAssignmentCreateRequest = components["schemas"]["AssistantAssignmentCreateRequest"];
type AssistantSearchResponse = components["schemas"]["AssistantSearchResponse"];

type AssistantListResponse = components["schemas"]["RsDataPageResponseAssistantAssignmentResponse"];
type UpdateAssistantResponse = components["schemas"]["RsDataAssistantAssignmentResponse"];
type AssistantSearchListResponse = components["schemas"]["RsDataListAssistantSearchResponse"];

type FetchAssistantsRsData = Required<AssistantListResponse>["data"];
type UpdateAssistantRsData = Required<UpdateAssistantResponse>["data"];
type AssistantSearchRsData = Required<AssistantSearchListResponse>["data"];

const pageSize = 10;

export default function TeacherAssistantsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return <TeacherAssistantsContent />;
}

function TeacherAssistantsContent() {
  const { showToast } = useToast();

  const [assistantStatus, setAssistantStatus] = useState<AssistantFilter>("ACTIVE");
  const [assistantPage, setAssistantPage] = useState(0);
  const [assistants, setAssistants] = useState<AssistantAssignmentResponse[]>([]);
  const [assistantTotal, setAssistantTotal] = useState(0);
  const [assistantLoading, setAssistantLoading] = useState(false);
  const [assistantError, setAssistantError] = useState<string | null>(null);
  const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);

  const fetchAssistants = useCallback(
    async (status: AssistantFilter, page: number) => {
      setAssistantLoading(true);
      setAssistantError(null);
      try {
        const response = await api.GET("/api/v1/teachers/me/assistants", {
          params: { query: { status, page, size: pageSize } }
        });
        if (response.error || !response.data?.data) {
          throw new Error(getApiErrorMessage(response.error, "조교 목록을 가져오지 못했습니다."));
        }
        const pageData = response.data.data as FetchAssistantsRsData;
        setAssistants((pageData?.content ?? []) as AssistantAssignmentResponse[]);
        setAssistantTotal(pageData?.totalElements ?? 0);
      } catch (error) {
        setAssistantError(error instanceof Error ? error.message : "조교 목록을 가져오지 못했습니다.");
      } finally {
        setAssistantLoading(false);
      }
    },
    []
  );

  const refreshAssistants = useCallback(() => {
    setAssistantPage(0);
    void fetchAssistants(assistantStatus, 0);
  }, [assistantStatus, fetchAssistants]);

  useEffect(() => {
    void fetchAssistants(assistantStatus, assistantPage);
  }, [assistantStatus, assistantPage, fetchAssistants]);

  const assistantEmptyMessage = useMemo(() => {
    switch (assistantStatus) {
      case "ACTIVE":
        return { message: "활성화된 조교가 없습니다", description: "조교 초대를 보내고 활성화하세요." };
      case "INACTIVE":
        return { message: "비활성화된 조교가 없습니다", description: "필요 시 조교를 비활성화할 수 있습니다." };
      default:
        return { message: "등록된 조교가 없습니다", description: "초대 관리 메뉴에서 초대를 생성하세요." };
    }
  }, [assistantStatus]);

  const handleToggleAssistant = async (assignmentId?: string, enabled?: boolean) => {
    if (!assignmentId || enabled === undefined) {
      return;
    }
    const previous = assistants;
    setAssistants((prev) =>
      prev.map((assistant) =>
        assistant.assignmentId === assignmentId ? { ...assistant, isActive: enabled } : assistant
      )
    );

    try {
      const requestBody: AssistantAssignmentStatusUpdateRequest = { enabled };
      const response = await api.PATCH("/api/v1/teachers/me/assistants/{assignmentId}", {
        params: { path: { assignmentId } },
        body: requestBody
      });
      if (response.error || !response.data?.data) {
        throw new Error(getApiErrorMessage(response.error, "조교 상태를 변경하지 못했습니다."));
      }
      showToast("success", enabled ? "조교를 활성화했습니다." : "조교를 비활성화했습니다.");
      await fetchAssistants(assistantStatus, assistantPage);
    } catch (error) {
      setAssistants(previous);
      const message = error instanceof Error ? error.message : "조교 상태 변경 중 오류가 발생했습니다.";
      setAssistantError(message);
      showToast("error", message);
    }
  };

  const assistantTotalPages = Math.ceil(assistantTotal / pageSize);

  return (
    <div className="space-y-6 lg:space-y-8">
      <PageHero onOpenSearch={() => setIsSearchModalOpen(true)} />

      <Card title="조교 목록" description="상태를 전환해 조교의 접근 권한을 조정합니다.">
        <Tabs
          defaultValue={assistantTabs[0].value}
          value={assistantStatus}
          onValueChange={(value) => {
            setAssistantStatus(value as AssistantFilter);
            setAssistantPage(0);
          }}
          className="mt-1"
        >
          <TabsList>
            {assistantTabs.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        <AssistantList
          assistants={assistants}
          loading={assistantLoading}
          error={assistantError}
          onToggle={handleToggleAssistant}
          emptyMessage={assistantEmptyMessage.message}
          emptyDescription={assistantEmptyMessage.description}
        />

        <Pagination
          currentPage={assistantPage}
          totalPages={assistantTotalPages}
          onPageChange={setAssistantPage}
        />
      </Card>

      <AssistantSearchModal
        open={isSearchModalOpen}
        onClose={() => setIsSearchModalOpen(false)}
        onRegistered={() => {
          setIsSearchModalOpen(false);
          refreshAssistants();
        }}
      />
    </div>
  );
}

function PageHero({ onOpenSearch }: { onOpenSearch: () => void }) {
  return (
    <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">Assistant Management</p>
          <h1 className="mt-2 text-3xl font-bold text-slate-900">조교 관리</h1>
          <p className="mt-2 text-sm text-slate-500">
            배정된 조교의 활성/비활성 상태를 전환하고, 이메일로 새 조교를 바로 연결할 수 있습니다.
          </p>
        </div>
        <Button onClick={onOpenSearch} className="w-full md:w-auto">
          조교 검색 및 등록
        </Button>
      </div>
    </section>
  );
}

function AssistantList({
  assistants,
  loading,
  error,
  onToggle,
  emptyMessage,
  emptyDescription
}: {
  assistants: AssistantAssignmentResponse[];
  loading: boolean;
  error: string | null;
  onToggle: (assignmentId?: string, enabled?: boolean) => void;
  emptyMessage: string;
  emptyDescription?: string;
}) {
  if (loading) {
    return <p className="mt-4 text-sm text-slate-500">조교 목록을 불러오는 중입니다…</p>;
  }

  if (error) {
    return <InlineError message={error} className="mt-4" />;
  }

  if (assistants.length === 0) {
    return (
      <EmptyState
        message={emptyMessage}
        description={emptyDescription}
        icon={<span className="text-2xl">📭</span>}
      />
    );
  }

  return (
    <ul className="mt-6 space-y-3">
      {assistants.map((assistant) => (
        <li
          key={assistant.assignmentId}
          className="flex flex-col gap-4 rounded-2xl border border-slate-100 px-4 py-3 shadow-sm sm:flex-row sm:items-center sm:justify-between"
        >
          <div>
            <p className="font-semibold text-slate-900">{assistant.assistant?.name ?? "이름 미확인"}</p>
            <p className="text-sm text-slate-500">{assistant.assistant?.email ?? "이메일 미확인"}</p>
            <p className="text-sm text-slate-500">
              {assistant.assistant?.phoneNumber
                ? formatPhoneNumber(assistant.assistant.phoneNumber)
                : "전화번호 미등록"}
            </p>
            <p className="text-xs text-slate-400">
              배정일 {assistant.assignedAt ? new Date(assistant.assignedAt).toLocaleString("ko-KR") : "알 수 없음"}
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <Badge variant={assistant.isActive ? "success" : "secondary"}>
              {assistant.isActive ? "활성" : "비활성"}
            </Badge>
            <Button
              variant={assistant.isActive ? "secondary" : "primary"}
              onClick={() => onToggle(assistant.assignmentId, !assistant.isActive)}
              className="min-w-[120px]"
            >
              {assistant.isActive ? "비활성화" : "활성화"}
            </Button>
          </div>
        </li>
      ))}
    </ul>
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
  if (!Number.isFinite(totalPages) || totalPages <= 1) {
    return null;
  }

  return (
    <div className="mt-6 flex items-center justify-between text-sm text-slate-500">
      <span>
        페이지 {currentPage + 1} / {totalPages}
      </span>
      <div className="flex gap-2">
        <Button variant="ghost" disabled={currentPage === 0} onClick={() => onPageChange(currentPage - 1)}>
          이전
        </Button>
        <Button variant="ghost" disabled={currentPage + 1 >= totalPages} onClick={() => onPageChange(currentPage + 1)}>
          다음
        </Button>
      </div>
    </div>
  );
}

function AssistantSearchModal({
  open,
  onClose,
  onRegistered
}: {
  open: boolean;
  onClose: () => void;
  onRegistered: () => void;
}) {
  const { showToast } = useToast();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [results, setResults] = useState<AssistantSearchResponse[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [selectedAssistant, setSelectedAssistant] = useState<AssistantSearchResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query, open]);

  useEffect(() => {
    setSelectedAssistant(null);
  }, [debouncedQuery]);

  useEffect(() => {
    if (!open) {
      setQuery("");
      setDebouncedQuery("");
      setResults([]);
      setSearchError(null);
      setSelectedAssistant(null);
      setIsSubmitting(false);
      return;
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const trimmed = debouncedQuery.trim();
    if (trimmed.length < 2) {
      setResults([]);
      setSearchError(null);
      return;
    }
    let cancelled = false;
    const fetchResults = async () => {
      setSearchLoading(true);
      setSearchError(null);
      try {
        const response = await api.GET("/api/v1/teachers/me/assistants/search", {
          params: { query: { email: trimmed } }
        });
        if (!response.data) {
          const apiError = (response as { error?: unknown }).error;
          throw new Error(getApiErrorMessage(apiError, "조교를 검색하지 못했습니다."));
        }
        const data = response.data.data as AssistantSearchRsData | undefined;
        if (!cancelled) {
          setResults(data ?? []);
        }
      } catch (error) {
        if (!cancelled) {
          const message = error instanceof Error ? error.message : "조교 검색 중 오류가 발생했습니다.";
          setSearchError(message);
        }
      } finally {
        if (!cancelled) {
          setSearchLoading(false);
        }
      }
    };
    void fetchResults();
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery, open]);

  const handleRegister = async () => {
    if (!selectedAssistant?.assistantMemberId) {
      return;
    }
    try {
      setIsSubmitting(true);
      const body: AssistantAssignmentCreateRequest = {
        assistantMemberId: selectedAssistant.assistantMemberId
      };
      const response = await api.POST("/api/v1/teachers/me/assistants", { body });
      if (response.error || !response.data?.data) {
        throw new Error(getApiErrorMessage(response.error, "조교를 등록하지 못했습니다."));
      }
      showToast("success", `${selectedAssistant.name ?? "조교"}님을 연결했습니다.`);
      onRegistered();
    } catch (error) {
      const message = error instanceof Error ? error.message : "조교 등록 중 오류가 발생했습니다.";
      showToast("error", message);
      setIsSubmitting(false);
    }
  };

  const canSearch = debouncedQuery.trim().length >= 2;
  const selectedStatus = selectedAssistant?.assignmentStatus;
  const registerDisabled =
    !selectedAssistant || selectedStatus !== "NOT_ASSIGNED" || isSubmitting || !selectedAssistant.assistantMemberId;

  return (
    <Modal
      open={open}
      onClose={() => {
        if (!isSubmitting) {
          onClose();
        }
      }}
      title="조교 검색 및 등록"
      size="lg"
      mobileLayout="bottom-sheet"
    >
      <div className="space-y-5">
        <div>
          <label className="text-sm font-medium text-slate-700">
            조교 이메일
            <Input
              className="mt-2"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="assistant@example.com"
              disabled={isSubmitting}
            />
          </label>
          <p className="mt-1 text-xs text-slate-500">두 글자 이상 입력하면 자동으로 검색을 시작합니다.</p>
        </div>

        {searchError ? <InlineError message={searchError} /> : null}
        {!canSearch && debouncedQuery.length === 0 ? (
          <p className="text-sm text-slate-500">이메일을 입력하면 검색 결과가 표시됩니다.</p>
        ) : null}

        <div className="max-h-72 space-y-2 overflow-y-auto rounded-2xl border border-slate-100 p-3">
          {searchLoading ? (
            <p className="text-sm text-slate-500">검색 중입니다…</p>
          ) : results.length === 0 && canSearch ? (
            <p className="text-sm text-slate-500">일치하는 조교가 없습니다.</p>
          ) : (
                results.map((assistant) => {
              const status = assistant.assignmentStatus ?? "NOT_ASSIGNED";
              const isSelected = selectedAssistant?.assistantMemberId === assistant.assistantMemberId;
              const badgeVariant = status === "NOT_ASSIGNED" ? "secondary" : status === "ACTIVE" ? "success" : "destructive";
              return (
                <button
                  key={assistant.assistantMemberId}
                  type="button"
                  onClick={() => setSelectedAssistant(assistant)}
                  className={clsx(
                    "w-full rounded-2xl border px-4 py-3 text-left transition",
                    isSelected ? "border-blue-500 bg-blue-50" : "border-slate-200 hover:border-blue-200",
                    !assistant.assistantMemberId && "opacity-60"
                  )}
                  disabled={!assistant.assistantMemberId}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="font-semibold text-slate-900">{assistant.name ?? "이름 미확인"}</p>
                      <p className="text-sm text-slate-500">{assistant.email ?? "이메일 미확인"}</p>
                    </div>
                    <Badge
                      variant={badgeVariant}
                      className="shrink-0"
                    >
                      {status === "NOT_ASSIGNED" ? "미배정" : status === "ACTIVE" ? "활성" : "비활성"}
                    </Badge>
                  </div>
                </button>
              );
            })
          )}
        </div>

        {selectedAssistant && (
          <p className="text-sm text-slate-500">
            선택된 조교: <span className="font-semibold text-slate-900">{selectedAssistant.name}</span>{" "}
            {selectedStatus !== "NOT_ASSIGNED" ? "(이미 연결됨)" : ""}
          </p>
        )}

        <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
            닫기
          </Button>
          <Button onClick={handleRegister} disabled={registerDisabled}>
            {isSubmitting ? "등록 중..." : "등록"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
