"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import type {
  StudentTeacherRequestResponse,
  StudentTeacherRequestStatus,
  TeacherBranchSummary,
  TeacherSearchResponse
} from "@/types/dashboard";
import {
  DASHBOARD_PAGE_SIZE,
  cancelStudentTeacherRequest,
  createStudentTeacherRequest,
  fetchStudentTeacherRequests,
  fetchTeacherSearch
} from "@/lib/dashboard-api";

type TabKey = "search" | "requests";

type Option = { value: string; label: string };

type TeacherSearchTabProps = {
  onRequestSuccess: () => void;
};

type RequestStatusOption = {
  value: StudentTeacherRequestStatus;
  label: string;
};

const requestStatusOptions: RequestStatusOption[] = [
  { value: "PENDING", label: "대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "거절" },
  { value: "CANCELLED", label: "취소" }
];

const statusBadgeVariant: Record<StudentTeacherRequestStatus, Parameters<typeof Badge>[0]["variant"]> = {
  PENDING: "secondary",
  APPROVED: "success",
  REJECTED: "destructive",
  CANCELLED: "secondary"
};

export default function StudentTeachersPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return <StudentTeachersContent />;
}

function StudentTeachersContent() {
  const searchParams = useSearchParams();
  const initialTab: TabKey = searchParams.get("tab") === "requests" ? "requests" : "search";
  const [activeTab, setActiveTab] = useState<TabKey>(initialTab);

  const handleTabChange = (value: string) => {
    setActiveTab(value === "requests" ? "requests" : "search");
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Student · Teacher Requests</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">선생님 관리</h1>
        <p className="mt-2 text-sm text-slate-500">
          선생님을 검색해 연결 요청을 보내고, 승인/거절 상태를 한 화면에서 확인하세요. 요청 내역은 언제든 취소할 수
          있습니다.
        </p>
      </header>

      <Tabs defaultValue={activeTab} value={activeTab} onValueChange={handleTabChange} className="space-y-4">
        <TabsList>
          <TabsTrigger value="search">선생님 검색</TabsTrigger>
          <TabsTrigger value="requests">신청 내역</TabsTrigger>
        </TabsList>

        <TabsContent value="search">
          <TeacherSearchTab onRequestSuccess={() => setActiveTab("requests")} />
        </TabsContent>
        <TabsContent value="requests">
          <TeacherRequestsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function TeacherSearchTab({ onRequestSuccess }: TeacherSearchTabProps) {
  const { showToast } = useToast();
  const [companyId, setCompanyId] = useState("");
  const [branchId, setBranchId] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const keyword = useDebounce(keywordInput.trim(), 300);
  const [page, setPage] = useState(0);

  const [teachers, setTeachers] = useState<TeacherSearchResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [companyOptions, setCompanyOptions] = useState<Option[]>([]);
  const [companyNameMap, setCompanyNameMap] = useState<Record<string, string>>({});
  const [branchOptionMap, setBranchOptionMap] = useState<Record<string, Record<string, string>>>({});

  const [selectedTeacher, setSelectedTeacher] = useState<TeacherSearchResponse | null>(null);
  const [requestMessage, setRequestMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchTeacherSearch({
        companyId: companyId || undefined,
        branchId: branchId || undefined,
        keyword: keyword || undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setTeachers(result.items);
      setTotal(result.totalElements);
      setCompanyOptions((prev) => {
        const map = new Map(prev.map((opt) => [opt.value, opt.label] as const));
        result.items.forEach((teacher) => {
          teacher.branches?.forEach((branch) => {
            if (branch.companyId) {
              map.set(branch.companyId, branch.companyName ?? "학원");
            }
          });
        });
        return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
      });
      setCompanyNameMap((prev) => {
        const next = { ...prev };
        result.items.forEach((teacher) => {
          teacher.branches?.forEach((branch) => {
            if (branch.companyId) {
              next[branch.companyId] = branch.companyName ?? "학원";
            }
          });
        });
        return next;
      });
      setBranchOptionMap((prev) => {
        const next: Record<string, Record<string, string>> = { ...prev };
        result.items.forEach((teacher) => {
          teacher.branches?.forEach((branch) => {
            if (branch.companyId && branch.branchId) {
              const existing = next[branch.companyId] ? { ...next[branch.companyId] } : {};
              existing[branch.branchId] = branch.branchName ?? "지점";
              next[branch.companyId] = existing;
            }
          });
        });
        return next;
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : "선생님 목록을 불러오지 못했습니다.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [branchId, companyId, keyword, page]);

  useEffect(() => {
    void loadTeachers();
  }, [loadTeachers]);

  const branchSelectOptions = useMemo(() => {
    if (companyId && branchOptionMap[companyId]) {
      return Object.entries(branchOptionMap[companyId]).map(([value, label]) => ({ value, label }));
    }
    const union = new Map<string, string>();
    Object.entries(branchOptionMap).forEach(([companyKey, branches]) => {
      const companyLabel = companyNameMap[companyKey];
      Object.entries(branches).forEach(([value, label]) => {
        const mergedLabel = companyLabel ? `${companyLabel} ${label}` : label;
        union.set(value, mergedLabel);
      });
    });
    return Array.from(union.entries()).map(([value, label]) => ({ value, label }));
  }, [branchOptionMap, companyId, companyNameMap]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const resetFilters = () => {
    setCompanyId("");
    setBranchId("");
    setKeywordInput("");
    setPage(0);
  };

  const handleOpenRequest = (teacher: TeacherSearchResponse) => {
    setSelectedTeacher(teacher);
    setRequestMessage("");
  };

  const closeModal = () => {
    setSelectedTeacher(null);
    setRequestMessage("");
  };

  const handleSubmitRequest = async () => {
    if (!selectedTeacher?.teacherId) {
      return;
    }
    setSubmitting(true);
    try {
      await createStudentTeacherRequest({
        teacherId: selectedTeacher.teacherId,
        message: requestMessage.trim().length > 0 ? requestMessage.trim() : undefined
      });
      showToast("success", "요청을 보냈습니다. 신청 내역에서 상태를 확인하세요.");
      closeModal();
      await loadTeachers();
      onRequestSuccess();
    } catch (err) {
      const message = err instanceof Error ? err.message : "요청을 보내지 못했습니다.";
      showToast("error", message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card title="검색 필터" description="이름과 학원/지점 필터를 조합해 연결할 선생님을 찾으세요.">
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
          <Field label="선생님 이름" className="flex-1">
            <Input
              placeholder="선생님 이름을 입력하세요."
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
                setPage(0);
              }}
            />
          </Field>
          <div className="flex items-end gap-3">
            <Button variant="secondary" onClick={resetFilters} disabled={loading}>
              초기화
            </Button>
            <Button onClick={() => void loadTeachers()} disabled={loading}>
              새로고침
            </Button>
          </div>
        </div>
        {error && <InlineError className="mt-4" message={error} />}
      </Card>

      <Card title="선생님 목록" description="요청 가능한 선생님만 표시됩니다. 이미 요청/연결된 선생님은 숨겨집니다.">
        {!loading && teachers.length === 0 && (
          <div className="py-12">
            <EmptyState
              message="표시할 선생님이 없습니다."
              description="필터나 검색어를 변경해서 다시 시도해 주세요."
            />
          </div>
        )}
        {loading && <p className="py-12 text-center text-sm text-slate-500">선생님 목록을 불러오는 중...</p>}
        {!loading && teachers.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2">
            {teachers.map((teacher, index) => {
              const key = teacher.teacherId ?? `teacher-${index}`;
              const branches = formatBranchLabels(teacher.branches);
              return (
                <article
                  key={key}
                  className="rounded-2xl border border-slate-200 p-5 shadow-sm transition hover:border-indigo-200 hover:bg-indigo-50/40"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Teacher</p>
                      <h3 className="mt-1 text-lg font-bold text-slate-900">
                        {teacher.teacherName ?? "이름 없는 선생님"}
                      </h3>
                      <p className="text-sm text-slate-500">요청 대상 선생님</p>
                    </div>
                    <Badge variant="secondary">요청 가능</Badge>
                  </div>
                  <div className="mt-4 space-y-2 text-sm text-slate-600">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">출강 지점</p>
                    {branches.length > 0 ? (
                      <div className="flex flex-wrap gap-2">
                        {branches.map((branch) => (
                          <span
                            key={branch}
                            className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600"
                          >
                            {branch}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <p className="text-xs text-slate-400">등록된 출강 지점 정보가 없습니다.</p>
                    )}
                  </div>
                  <Button
                    className="mt-4 w-full"
                    onClick={() => handleOpenRequest(teacher)}
                    disabled={!teacher.teacherId}
                  >
                    연결 요청
                  </Button>
                </article>
              );
            })}
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <TeacherRequestModal
        open={Boolean(selectedTeacher)}
        teacher={selectedTeacher}
        message={requestMessage}
        onMessageChange={setRequestMessage}
        onConfirm={handleSubmitRequest}
        onClose={closeModal}
        loading={submitting}
      />
    </div>
  );
}

function TeacherRequestsTab() {
  const { showToast } = useToast();
  const [statuses, setStatuses] = useState<StudentTeacherRequestStatus[]>(["PENDING"]);
  const [page, setPage] = useState(0);
  const [requests, setRequests] = useState<StudentTeacherRequestResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<StudentTeacherRequestResponse | null>(null);
  const [cancelLoading, setCancelLoading] = useState(false);

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchStudentTeacherRequests({
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

  const toggleStatus = (value: StudentTeacherRequestStatus, checked: boolean) => {
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
      await cancelStudentTeacherRequest(cancelTarget.requestId);
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
          {requestStatusOptions.map((option) => (
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

      <Card title="신청 내역" description="진행 중/완료된 요청을 모두 확인할 수 있습니다.">
        {loading && <p className="py-12 text-center text-sm text-slate-500">신청 내역을 불러오는 중입니다...</p>}
        {!loading && requests.length === 0 && (
          <div className="py-12">
            <EmptyState
              message="등록된 신청이 없습니다."
              description="선생님 검색 탭에서 요청을 보내면 이곳에서 상태를 확인할 수 있습니다."
            />
          </div>
        )}
        {!loading && requests.length > 0 && (
          <div className="space-y-4">
            {requests.map((request, index) => {
              const key = request.requestId ?? `request-${index}`;
              const status = request.status ?? "PENDING";
              const cancellable = status === "PENDING" && Boolean(request.requestId);
              const branches = formatBranchLabels(request.teacher?.branches);
              return (
                <article key={key} className="rounded-2xl border border-slate-200 p-5 shadow-sm">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                        신청일 {formatDate(request.createdAt)}
                      </p>
                      <h3 className="mt-1 text-lg font-semibold text-slate-900">
                        {request.teacher?.teacherName ?? "이름 없는 선생님"}
                      </h3>
                      <p className="text-sm text-slate-500">
                        {branches.length > 0 ? branches.join(", ") : "출강 지점 정보 없음"}
                      </p>
                    </div>
                    <Badge variant={statusBadgeVariant[status]}>{statusToLabel(status)}</Badge>
                  </div>
                  <dl className="mt-4 space-y-2 text-sm text-slate-600">
                    <div className="flex gap-2">
                      <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">메시지</dt>
                      <dd className="text-slate-500">{request.message ?? "남긴 메시지가 없습니다."}</dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="w-16 text-xs font-semibold uppercase tracking-wide text-slate-400">처리일</dt>
                      <dd className="text-slate-500">{formatDate(request.processedAt)}</dd>
                    </div>
                  </dl>
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
                    <span>처리자 {request.processedByMemberId ?? "-"}</span>
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
        message="해당 신청을 취소하면 다시 요청을 보내야 합니다. 취소하시겠습니까?"
        confirmText="취소하기"
        cancelText="닫기"
      />
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
    <label className={clsx("flex w-full flex-col gap-1 text-sm font-semibold text-slate-700", className)}>
      <span>{label}</span>
      {children}
    </label>
  );
}

type TeacherRequestModalProps = {
  open: boolean;
  teacher: TeacherSearchResponse | null;
  message: string;
  onMessageChange: (value: string) => void;
  onConfirm: () => void;
  onClose: () => void;
  loading: boolean;
};

function TeacherRequestModal({
  open,
  teacher,
  message,
  onMessageChange,
  onConfirm,
  onClose,
  loading
}: TeacherRequestModalProps) {
  const branches = formatBranchLabels(teacher?.branches);
  return (
    <Modal open={open} onClose={loading ? () => undefined : onClose} title="연결 요청 확인" size="lg">
      {teacher ? (
        <div className="space-y-6">
          <section>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">선택한 선생님</p>
            <div className="mt-2 rounded-2xl border border-slate-200/70 bg-slate-50 px-4 py-3">
              <h3 className="text-lg font-bold text-slate-900">{teacher.teacherName ?? "이름 없는 선생님"}</h3>
              <p className="text-sm text-slate-500">
                {branches.length > 0 ? branches.join(", ") : "출강 지점 정보가 없습니다."}
              </p>
            </div>
          </section>
          <section>
            <label className="flex flex-col gap-2 text-sm font-semibold text-slate-700">
              <span>전달 메시지 (선택)</span>
              <textarea
                className="h-32 rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-200 disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="선생님께 전달하고 싶은 내용을 적어주세요."
                value={message}
                onChange={(event) => onMessageChange(event.target.value)}
                disabled={loading}
              />
            </label>
          </section>
          <p className="text-xs text-slate-500">
            요청이 완료되면 신청 내역에서 상태를 확인할 수 있습니다. 승인 전까지는 언제든 취소할 수 있습니다.
          </p>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={onClose} disabled={loading}>
              닫기
            </Button>
            <Button onClick={onConfirm} disabled={loading}>
              {loading ? "요청 중..." : "요청 보내기"}
            </Button>
          </div>
        </div>
      ) : (
        <p className="text-sm text-slate-600">선택된 선생님이 없습니다.</p>
      )}
    </Modal>
  );
}

function formatBranchLabels(branches?: TeacherBranchSummary[]) {
  if (!branches) return [];
  return branches.map((branch) => {
    const company = branch.companyName ?? "학원";
    const branchName = branch.branchName ?? "지점";
    return `${company} ${branchName}`.trim();
  });
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  try {
    return new Intl.DateTimeFormat("ko", { dateStyle: "medium" }).format(new Date(value));
  } catch {
    return value;
  }
}

function statusToLabel(status: StudentTeacherRequestStatus) {
  return requestStatusOptions.find((option) => option.value === status)?.label ?? status;
}
