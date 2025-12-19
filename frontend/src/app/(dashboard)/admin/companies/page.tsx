"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { useToast } from "@/components/ui/toast";
import type { CompanyResponse, VerificationFilter } from "@/types/dashboard";
import {
  DASHBOARD_PAGE_SIZE,
  fetchAdminCompanies,
  updateAdminCompanyStatus
} from "@/lib/dashboard-api";

const verificationTabs: { label: string; value: VerificationFilter }[] = [
  { label: "미검증", value: "UNVERIFIED" },
  { label: "검증 완료", value: "VERIFIED" },
  { label: "전체", value: "ALL" }
];

export default function AdminCompanyVerificationPage() {
  const { canRender, fallback } = useRoleGuard("SUPER_ADMIN");
  if (!canRender) {
    return fallback;
  }
  return <CompanyVerificationContent />;
}

function CompanyVerificationContent() {
  const { showToast } = useToast();
  const [status, setStatus] = useState<VerificationFilter>("UNVERIFIED");
  const [page, setPage] = useState(0);
  const [companies, setCompanies] = useState<CompanyResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCompanies = useCallback(
    async (targetStatus: VerificationFilter, targetPage: number) => {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchAdminCompanies({ status: targetStatus, page: targetPage });
        setCompanies(result.items);
        setTotal(result.totalElements);
      } catch (err) {
        const message = err instanceof Error ? err.message : "회사 목록을 불러오지 못했습니다.";
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadCompanies(status, page);
  }, [status, page, loadCompanies]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const emptyDescription = useMemo(() => {
    switch (status) {
      case "UNVERIFIED":
        return "새로 등록된 학원을 기다리는 중입니다.";
      case "VERIFIED":
        return "검증 완료된 학원이 아직 없습니다.";
      default:
        return "등록된 학원이 없습니다.";
    }
  }, [status]);

  const handleToggle = async (company: CompanyResponse) => {
    if (!company.companyId) return;
    const nextVerified = company.verifiedStatus !== "VERIFIED";
    try {
      await updateAdminCompanyStatus(company.companyId, {
        verified: nextVerified,
        enabled: true
      });
      showToast("success", nextVerified ? "회사를 VERIFIED 처리했습니다." : "회사를 UNVERIFIED 처리했습니다.");
      await loadCompanies(status, page);
    } catch (err) {
      const message = err instanceof Error ? err.message : "상태 변경에 실패했습니다.";
      setError(message);
      showToast("error", message);
    }
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Verification</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">회사 검증</h1>
        <p className="mt-2 text-sm text-slate-500">새로 등록된 학원을 검토하고 상태를 변경하세요.</p>
      </header>

      <Card
        title="회사 목록"
        description="상태 탭을 전환해 검증 대기/완료 학원을 확인하고 승인 절차를 진행하세요."
      >
        <Tabs
          defaultValue={verificationTabs[0].value}
          value={status}
          onValueChange={(value) => {
            setStatus(value as VerificationFilter);
            setPage(0);
          }}
        >
          <TabsList>
            {verificationTabs.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        {error && <InlineError message={error} className="mt-4" />}

        <div className="mt-4 divide-y divide-slate-100">
          {!loading && companies.length === 0 && (
            <EmptyState message="표시할 학원이 없습니다." description={emptyDescription} />
          )}
          {companies.map((company) => (
            <div
              key={company.companyId}
              className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="text-base font-semibold text-slate-900">{company.name}</p>
                <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500">
                  <Badge variant={company.verifiedStatus === "VERIFIED" ? "success" : "secondary"}>
                    {company.verifiedStatus === "VERIFIED" ? "VERIFIED" : "UNVERIFIED"}
                  </Badge>
                  <Badge variant="secondary">{company.type === "INDIVIDUAL" ? "개인" : "회사"}</Badge>
                  {company.creatorMemberId && (
                    <span>등록자: <code className="text-xs">{company.creatorMemberId}</code></span>
                  )}
                  {company.createdAt && <span>등록일: {formatDate(company.createdAt)}</span>}
                </div>
                {company.description && <p className="mt-2 text-sm text-slate-500">{company.description}</p>}
              </div>
              <div className="flex gap-3">
                <Button
                  variant={company.verifiedStatus === "VERIFIED" ? "secondary" : "primary"}
                  onClick={() => void handleToggle(company)}
                  disabled={loading}
                >
                  {company.verifiedStatus === "VERIFIED" ? "UNVERIFIED 처리" : "VERIFIED 처리"}
                </Button>
              </div>
            </div>
          ))}
        </div>

        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>
    </div>
  );
}

function formatDate(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("ko", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
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
