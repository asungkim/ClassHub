"use client";

import clsx from "clsx";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { useToast } from "@/components/ui/toast";
import { Modal } from "@/components/ui/modal";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import type {
  BranchResponse,
  CompanyResponse,
  TeacherAssignmentFilter,
  TeacherBranchAssignment,
  TeacherBranchAssignmentCreateRequest
} from "@/types/dashboard";
import {
  DASHBOARD_PAGE_SIZE,
  createTeacherBranchAssignment,
  fetchTeacherBranchAssignments,
  searchBranches,
  searchTeacherCompanies,
  updateTeacherBranchAssignmentStatus
} from "@/lib/dashboard-api";

const assignmentTabs: { label: string; value: TeacherAssignmentFilter }[] = [
  { label: "활성", value: "ACTIVE" },
  { label: "비활성", value: "INACTIVE" },
  { label: "전체", value: "ALL" }
];

type FlowType = "INDIVIDUAL" | "COMPANY";
type AssignmentMode = TeacherBranchAssignmentCreateRequest["mode"];
type BranchRole = NonNullable<TeacherBranchAssignmentCreateRequest["role"]>;
type SelectionMode = "SEARCH" | "MANUAL";

const roleOptions: { value: BranchRole; label: string; description: string }[] = [
  {
    value: "OWNER",
    label: "대표 (OWNER)",
    description: "해당 학원을 대표/운영자로 등록합니다."
  },
  {
    value: "FREELANCE",
    label: "출강 강사",
    description: "외부 출강 또는 프리랜서 계약으로 등록합니다."
  }
];

export default function TeacherCompaniesPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return <TeacherBranchAssignmentsContent />;
}

function TeacherBranchAssignmentsContent() {
  const { showToast } = useToast();
  const [status, setStatus] = useState<TeacherAssignmentFilter>("ACTIVE");
  const [page, setPage] = useState(0);
  const [assignments, setAssignments] = useState<TeacherBranchAssignment[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const loadAssignments = useCallback(
    async (targetStatus: TeacherAssignmentFilter, targetPage: number) => {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchTeacherBranchAssignments({ status: targetStatus, page: targetPage });
        setAssignments(result.items);
        setTotalElements(result.totalElements);
      } catch (err) {
        const message = err instanceof Error ? err.message : "지점 목록을 불러오지 못했습니다.";
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadAssignments(status, page);
  }, [status, page, loadAssignments]);

  const totalPages = Math.ceil(totalElements / DASHBOARD_PAGE_SIZE);

  const emptyDescriptions = useMemo(() => {
    switch (status) {
      case "ACTIVE":
        return "현재 출강 중인 학원이 없습니다. 학원 등록 버튼으로 새 지점을 연결하세요.";
      case "INACTIVE":
        return "비활성화된 학원이 없습니다.";
      default:
        return "등록된 출강 학원이 없습니다.";
    }
  }, [status]);

  const handleToggleAssignment = async (assignment: TeacherBranchAssignment, enabled: boolean) => {
    if (!assignment.assignmentId) {
      return;
    }
    setUpdatingId(assignment.assignmentId);
    try {
      await updateTeacherBranchAssignmentStatus({
        assignmentId: assignment.assignmentId,
        body: { enabled }
      });
      showToast("success", enabled ? "학원을 다시 활성화했습니다." : "학원을 비활성화했습니다.");
      await loadAssignments(status, page);
    } catch (err) {
      const message = err instanceof Error ? err.message : "상태 변경 중 오류가 발생했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">Academy Management</p>
            <h1 className="mt-2 text-3xl font-bold text-slate-900">학원 관리</h1>
            <p className="mt-2 text-sm text-slate-500">
              출강 나가는 학원을 등록하고, 더 이상 나가지 않는 지점은 비활성화하세요.
            </p>
          </div>
          <Button className="w-full md:w-auto" onClick={() => setIsModalOpen(true)}>
            학원 등록
          </Button>
        </div>
      </section>

      <Card
        title="출강 학원 목록"
        description="연결된 지점을 상태별로 살펴보고 필요 시 활성/비활성 상태를 전환하세요."
      >
        <Tabs
          defaultValue={assignmentTabs[0].value}
          value={status}
          onValueChange={(value) => {
            setStatus(value as TeacherAssignmentFilter);
            setPage(0);
          }}
        >
          <TabsList>
            {assignmentTabs.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        <AssignmentList
          assignments={assignments}
          loading={loading}
          error={error}
          emptyDescription={emptyDescriptions}
          onToggle={handleToggleAssignment}
          updatingId={updatingId}
        />

        <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
      </Card>

      <TeacherBranchAssignmentModal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onCreated={async () => {
          setIsModalOpen(false);
          await loadAssignments(status, 0);
          setPage(0);
        }}
      />
    </div>
  );
}

function AssignmentList({
  assignments,
  loading,
  error,
  emptyDescription,
  onToggle,
  updatingId
}: {
  assignments: TeacherBranchAssignment[];
  loading: boolean;
  error: string | null;
  emptyDescription: string;
  onToggle: (assignment: TeacherBranchAssignment, enabled: boolean) => void;
  updatingId: string | null;
}) {
  if (loading) {
    return <p className="mt-6 text-sm text-slate-500">지점 정보를 불러오는 중입니다…</p>;
  }
  if (error) {
    return <InlineError message={error} className="mt-6" />;
  }
  if (assignments.length === 0) {
    return (
      <div className="mt-6">
        <EmptyState message="표시할 지점이 없습니다." description={emptyDescription} />
      </div>
    );
  }

  return (
    <ul className="mt-6 space-y-4">
      {assignments.map((assignment) => {
        const isActive = !assignment.deletedAt;
        const verified = assignment.verifiedStatus === "VERIFIED";
        return (
          <li
            key={assignment.assignmentId}
            className="flex flex-col gap-4 rounded-2xl border border-slate-100 px-4 py-4 shadow-sm md:flex-row md:items-center md:justify-between"
          >
            <div className="space-y-2">
              <div className="flex flex-wrap items-center gap-2">
                <p className="text-base font-semibold text-slate-900">{assignment.companyName ?? "이름 미정 학원"}</p>
                <Badge variant={assignment.companyType === "INDIVIDUAL" ? "secondary" : "default"}>
                  {assignment.companyType === "INDIVIDUAL" ? "개인" : "회사"}
                </Badge>
                <Badge variant="secondary">
                  {assignment.role === "OWNER" ? "대표" : "출강 강사"}
                </Badge>
              </div>
              <p className="text-sm font-medium text-slate-700">{assignment.branchName ?? "지점명 미입력"}</p>
              <div className="text-xs text-slate-500">
                {!isActive && assignment.deletedAt ? (
                  <span className="ml-3 text-rose-500">비활성화 {formatDate(assignment.deletedAt)}</span>
                ) : null}
              </div>
            </div>
            <div className="flex flex-col items-start gap-2 md:items-end">
              <Badge variant={isActive ? "success" : "secondary"}>{isActive ? "활성" : "비활성"}</Badge>
              <Button
                variant={isActive ? "secondary" : "primary"}
                onClick={() => onToggle(assignment, !isActive)}
                disabled={!assignment.assignmentId || updatingId === assignment.assignmentId}
                className="min-w-[140px]"
              >
                {isActive ? "비활성화" : "활성화"}
              </Button>
            </div>
          </li>
        );
      })}
    </ul>
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
  if (!Number.isFinite(totalPages) || totalPages <= 1) {
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
          disabled={disabled || currentPage === 0}
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
        >
          이전
        </Button>
        <Button
          variant="secondary"
          className="h-10 px-4 text-xs"
          disabled={disabled || currentPage + 1 >= totalPages}
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
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
    return new Intl.DateTimeFormat("ko", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
  } catch {
    return value;
  }
}

type AssignmentModalProps = {
  open: boolean;
  onClose: () => void;
  onCreated: () => void | Promise<void>;
};

function TeacherBranchAssignmentModal({ open, onClose, onCreated }: AssignmentModalProps) {
  const { showToast } = useToast();
  const [flowType, setFlowType] = useState<FlowType>("INDIVIDUAL");
  const [companySelectionMode, setCompanySelectionMode] = useState<SelectionMode>("SEARCH");
  const [branchSelectionMode, setBranchSelectionMode] = useState<SelectionMode>("SEARCH");
  const [role, setRole] = useState<BranchRole>("FREELANCE");

  const [individualCompanyName, setIndividualCompanyName] = useState("");
  const [individualBranchName, setIndividualBranchName] = useState("");

  const [companyName, setCompanyName] = useState("");
  const [companyBranchName, setCompanyBranchName] = useState("");

  const [selectedCompany, setSelectedCompany] = useState<CompanyResponse | null>(null);
  const [selectedBranch, setSelectedBranch] = useState<BranchResponse | null>(null);
  const [newBranchName, setNewBranchName] = useState("");

  const [companyOptions, setCompanyOptions] = useState<CompanyResponse[]>([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [companyError, setCompanyError] = useState<string | null>(null);

  const [branchOptions, setBranchOptions] = useState<BranchResponse[]>([]);
  const [branchLoading, setBranchLoading] = useState(false);
  const [branchError, setBranchError] = useState<string | null>(null);

  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const resetForm = useCallback(() => {
    setFlowType("INDIVIDUAL");
    setCompanySelectionMode("SEARCH");
    setBranchSelectionMode("SEARCH");
    setRole("FREELANCE");
    setIndividualCompanyName("");
    setIndividualBranchName("");
    setCompanyName("");
    setCompanyBranchName("");
    setSelectedCompany(null);
    setSelectedBranch(null);
    setNewBranchName("");
    setCompanyOptions([]);
    setBranchOptions([]);
    setCompanyError(null);
    setBranchError(null);
    setFormError(null);
    setSubmitting(false);
  }, []);

  useEffect(() => {
    if (!open) {
      resetForm();
    }
  }, [open, resetForm]);

  useEffect(() => {
    if (!open || flowType !== "COMPANY" || companySelectionMode !== "SEARCH") {
      setCompanyOptions([]);
      setCompanyError(null);
      setCompanyLoading(false);
      return;
    }
    let cancelled = false;
    const loadCompanies = async () => {
      setCompanyLoading(true);
      setCompanyError(null);
      try {
        const result = await searchTeacherCompanies({
          size: 100
        });
        if (!cancelled) {
          setCompanyOptions(result.items);
        }
      } catch (error) {
        if (!cancelled) {
          const message = error instanceof Error ? error.message : "회사 목록을 불러오지 못했습니다.";
          setCompanyError(message);
        }
      } finally {
        if (!cancelled) {
          setCompanyLoading(false);
        }
      }
    };
    void loadCompanies();
    return () => {
      cancelled = true;
    };
  }, [flowType, open, companySelectionMode]);

  useEffect(() => {
    if (
      !open ||
      flowType !== "COMPANY" ||
      companySelectionMode !== "SEARCH" ||
      branchSelectionMode !== "SEARCH" ||
      !selectedCompany?.companyId
    ) {
      setBranchOptions([]);
      setBranchError(null);
      setBranchLoading(false);
      return;
    }
    let cancelled = false;
    const loadBranches = async () => {
      setBranchLoading(true);
      setBranchError(null);
      try {
        const result = await searchBranches({
          companyId: selectedCompany.companyId,
          size: 100
        });
        if (!cancelled) {
          setBranchOptions(result.items);
        }
      } catch (error) {
        if (!cancelled) {
          const message = error instanceof Error ? error.message : "지점 목록을 불러오지 못했습니다.";
          setBranchError(message);
        }
      } finally {
        if (!cancelled) {
          setBranchLoading(false);
        }
      }
    };
    void loadBranches();
    return () => {
      cancelled = true;
    };
  }, [
    branchSelectionMode,
    companySelectionMode,
    flowType,
    open,
    selectedCompany?.companyId
  ]);

  const isFormReady = useMemo(() => {
    if (flowType === "INDIVIDUAL") {
      return individualCompanyName.trim().length > 0 && individualBranchName.trim().length > 0;
    }
    if (companySelectionMode === "MANUAL") {
      return companyName.trim().length > 0 && companyBranchName.trim().length > 0;
    }
    if (!selectedCompany?.companyId) {
      return false;
    }
    if (branchSelectionMode === "SEARCH") {
      return Boolean(selectedBranch?.branchId);
    }
    return newBranchName.trim().length > 0;
  }, [
    branchSelectionMode,
    companyBranchName,
    companyName,
    companySelectionMode,
    flowType,
    individualBranchName,
    individualCompanyName,
    newBranchName,
    selectedBranch?.branchId,
    selectedCompany?.companyId
  ]);

  const handleCompanySelect = (company: CompanyResponse) => {
    setSelectedCompany(company);
    setSelectedBranch(null);
    setBranchSelectionMode("SEARCH");
  };

  const handleEnableManualCompany = () => {
    setCompanySelectionMode("MANUAL");
    setBranchSelectionMode("MANUAL");
    setSelectedCompany(null);
    setSelectedBranch(null);
    setNewBranchName("");
  };

  const handleBackToCompanySearch = () => {
    setCompanySelectionMode("SEARCH");
    setBranchSelectionMode("SEARCH");
    setCompanyName("");
    setCompanyBranchName("");
    setSelectedCompany(null);
    setSelectedBranch(null);
  };

  const handleEnableManualBranch = () => {
    setBranchSelectionMode("MANUAL");
    setSelectedBranch(null);
  };

  const handleBackToBranchSearch = () => {
    setBranchSelectionMode("SEARCH");
    setSelectedBranch(null);
    setNewBranchName("");
  };

  const buildPayload = (): TeacherBranchAssignmentCreateRequest | null => {
    if (flowType === "INDIVIDUAL") {
      const name = individualCompanyName.trim();
      const branch = individualBranchName.trim();
      if (!name || !branch) {
        setFormError("개인 학원 이름과 지점명을 모두 입력해주세요.");
        return null;
      }
      return {
        mode: "NEW_INDIVIDUAL",
        role: "OWNER",
        individual: {
          companyName: name,
          branchName: branch
        }
      };
    }

    if (companySelectionMode === "MANUAL") {
      const name = companyName.trim();
      const branch = companyBranchName.trim();
      if (!name || !branch) {
        setFormError("회사명과 첫 지점명을 모두 입력해주세요.");
        return null;
      }
      return {
        mode: "NEW_COMPANY",
        role,
        company: {
          companyName: name,
          branchName: branch
        }
      };
    }

    if (!selectedCompany?.companyId) {
      setFormError("회사를 선택해주세요.");
      return null;
    }

    if (branchSelectionMode === "SEARCH") {
      if (!selectedBranch?.branchId) {
        setFormError("지점을 선택해주세요.");
        return null;
      }
      return {
        mode: "EXISTING_BRANCH",
        branchId: selectedBranch.branchId,
        role
      };
    }

    const branchName = newBranchName.trim();
    if (!branchName) {
      setFormError("지점명을 입력해주세요.");
      return null;
    }
    return {
      mode: "NEW_BRANCH",
      role,
      branch: {
        companyId: selectedCompany.companyId,
        branchName
      }
    };
  };

  const handleSubmit = async () => {
    const payload = buildPayload();
    if (!payload) {
      return;
    }
    try {
      setSubmitting(true);
      setFormError(null);
      await createTeacherBranchAssignment(payload);
      showToast("success", "학원 연결을 완료했습니다.");
      await onCreated();
    } catch (error) {
      const message = error instanceof Error ? error.message : "학원 연결 중 오류가 발생했습니다.";
      setFormError(message);
      showToast("error", message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    if (submitting) {
      return;
    }
    onClose();
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="학원 등록"
      size="lg"
      mobileLayout="bottom-sheet"
    >
      <div className="space-y-6">
        <div className="space-y-2">
          <p className="text-sm font-semibold text-slate-700">등록 유형</p>
          <Tabs
            defaultValue="INDIVIDUAL"
            value={flowType}
            onValueChange={(value) => setFlowType(value as FlowType)}
          >
            <TabsList className="w-full justify-between">
              <TabsTrigger value="INDIVIDUAL" className="flex-1">
                개인 학원
              </TabsTrigger>
              <TabsTrigger value="COMPANY" className="flex-1">
                대형 학원
              </TabsTrigger>
            </TabsList>
          </Tabs>
          <p className="text-xs text-slate-500">
            개인 학원은 회사명/지점명을 직접 입력하며 자동으로 대표 권한으로 연결됩니다.
          </p>
        </div>

        {flowType === "INDIVIDUAL" ? (
          <div className="space-y-5">
            <div>
              <label className="text-sm font-semibold text-slate-700">
                학원 이름
                <Input
                  className="mt-2"
                  placeholder="예) 새싹수학 개인교습소"
                  value={individualCompanyName}
                  onChange={(event) => setIndividualCompanyName(event.target.value)}
                  disabled={submitting}
                />
              </label>
            </div>
            <div>
              <label className="text-sm font-semibold text-slate-700">
                지점 이름
                <Input
                  className="mt-2"
                  placeholder="예) 본점"
                  value={individualBranchName}
                  onChange={(event) => setIndividualBranchName(event.target.value)}
                  disabled={submitting}
                />
              </label>
              <p className="mt-1 text-xs text-slate-500">지점명이 없다면 ‘본점’처럼 간단히 작성해 주세요.</p>
            </div>
          </div>
        ) : (
          <div className="space-y-5">
            <div className="rounded-2xl border border-blue-100 bg-blue-50/50 px-4 py-3">
              <p className="text-sm font-semibold text-blue-900">역할: 출강 강사 </p>
              <p className="mt-1 text-xs text-blue-700">대형 학원에 출강하는 경우 자동으로 출강 강사로 등록됩니다.</p>
            </div>

            <section className="space-y-2">
              <p className="text-sm font-semibold text-slate-700">회사</p>
              {companySelectionMode === "MANUAL" ? (
                <>
                  <Input
                    className="mt-2"
                    placeholder="예) 클래스허브 어학원"
                    value={companyName}
                    onChange={(event) => setCompanyName(event.target.value)}
                    disabled={submitting}
                  />
                  <Input
                    className="mt-2"
                    placeholder="예) 강남 캠퍼스"
                    value={companyBranchName}
                    onChange={(event) => setCompanyBranchName(event.target.value)}
                    disabled={submitting}
                  />
                  <button
                    type="button"
                    className="text-xs font-semibold text-blue-600 hover:underline disabled:text-slate-400"
                    onClick={handleBackToCompanySearch}
                    disabled={submitting}
                  >
                    드롭다운에서 선택하기
                  </button>
                  <p className="text-xs text-slate-500">
                    회사와 첫 지점을 함께 등록합니다. 지점명은 가장 먼저 출강하는 지점으로 입력하세요.
                  </p>
                </>
              ) : (
                <>
                  <Select
                    value={selectedCompany?.companyId ?? ""}
                    onChange={(event) => {
                      const companyId = event.target.value;
                      if (companyId === "__manual__") {
                        handleEnableManualCompany();
                        return;
                      }
                      if (!companyId) {
                        setSelectedCompany(null);
                        return;
                      }
                      const company = companyOptions.find((c) => c.companyId === companyId);
                      if (company) {
                        handleCompanySelect(company);
                      }
                    }}
                    disabled={submitting || companyLoading}
                  >
                    <option value="">회사를 선택하세요</option>
                    {companyOptions.map((company) => (
                      <option key={company.companyId} value={company.companyId}>
                        {company.name}
                      </option>
                    ))}
                    <option value="__manual__">직접 입력</option>
                  </Select>
                  {companyError ? <InlineError message={companyError} /> : null}
                  {companyLoading ? <p className="text-xs text-slate-500">회사 목록을 불러오는 중입니다…</p> : null}
                </>
              )}
            </section>

            {companySelectionMode === "SEARCH" ? (
              <section className="space-y-2">
                <p className="text-sm font-semibold text-slate-700">지점</p>
                {branchSelectionMode === "MANUAL" ? (
                  <>
                    <Input
                      className="mt-2"
                      placeholder="새 지점명을 입력하세요"
                      value={newBranchName}
                      onChange={(event) => setNewBranchName(event.target.value)}
                      disabled={submitting || !selectedCompany}
                    />
                    <button
                      type="button"
                      className="text-xs font-semibold text-blue-600 hover:underline disabled:text-slate-400"
                      onClick={handleBackToBranchSearch}
                      disabled={submitting || !selectedCompany}
                    >
                      드롭다운에서 선택하기
                    </button>
                  </>
                ) : (
                  <>
                    <Select
                      value={selectedBranch?.branchId ?? ""}
                      onChange={(event) => {
                        const branchId = event.target.value;
                        if (branchId === "__manual__") {
                          handleEnableManualBranch();
                          return;
                        }
                        if (!branchId) {
                          setSelectedBranch(null);
                          return;
                        }
                        const branch = branchOptions.find((b) => b.branchId === branchId);
                        if (branch) {
                          setSelectedBranch(branch);
                        }
                      }}
                      disabled={submitting || !selectedCompany || branchLoading}
                    >
                      <option value="">지점을 선택하세요</option>
                      {branchOptions.map((branch) => (
                        <option key={branch.branchId} value={branch.branchId}>
                          {branch.name}
                        </option>
                      ))}
                      <option value="__manual__">직접 입력</option>
                    </Select>
                    {branchError ? <InlineError message={branchError} /> : null}
                    {branchLoading ? <p className="text-xs text-slate-500">지점 목록을 불러오는 중입니다…</p> : null}
                    {!selectedCompany ? (
                      <p className="text-xs text-slate-500">회사를 먼저 선택하면 지점을 선택할 수 있습니다.</p>
                    ) : null}
                  </>
                )}
              </section>
            ) : (
              <p className="text-xs text-slate-500">
                회사와 첫 지점을 직접 입력합니다. 지점명 입력란에 첫 출강 지점을 작성하세요.
              </p>
            )}
          </div>
        )}

        {formError ? <InlineError message={formError} /> : null}

        <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
          <Button variant="ghost" onClick={handleClose} disabled={submitting}>
            취소
          </Button>
          <Button onClick={() => void handleSubmit()} disabled={submitting || !isFormReady}>
            {submitting ? "등록 중..." : "등록"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
