"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useToast } from "@/components/ui/toast";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import type { FeedbackFilter, FeedbackResponse } from "@/types/dashboard";
import { DASHBOARD_PAGE_SIZE, fetchAdminFeedbacks, resolveFeedback } from "@/lib/dashboard-api";

const statusTabs: { label: string; value: FeedbackFilter }[] = [
  { label: "미해결", value: "SUBMITTED" },
  { label: "해결됨", value: "RESOLVED" },
  { label: "전체", value: "ALL" }
];

export default function AdminFeedbackPage() {
  const { canRender, fallback } = useRoleGuard("SUPER_ADMIN");
  if (!canRender) {
    return fallback;
  }
  return <AdminFeedbackContent />;
}

function AdminFeedbackContent() {
  const { showToast } = useToast();
  const [status, setStatus] = useState<FeedbackFilter>("SUBMITTED");
  const [page, setPage] = useState(0);
  const [feedbacks, setFeedbacks] = useState<FeedbackResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resolveTarget, setResolveTarget] = useState<FeedbackResponse | null>(null);
  const [isResolving, setIsResolving] = useState(false);

  const loadFeedbacks = useCallback(
    async (targetStatus: FeedbackFilter, targetPage: number) => {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchAdminFeedbacks({ status: targetStatus, page: targetPage });
        setFeedbacks(result.items);
        setTotal(result.totalElements);
      } catch (err) {
        const message = err instanceof Error ? err.message : "피드백 목록을 불러오지 못했습니다.";
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadFeedbacks(status, page);
  }, [status, page, loadFeedbacks]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const emptyDescription = useMemo(() => {
    switch (status) {
      case "SUBMITTED":
        return "해결 대기 중인 피드백이 없습니다.";
      case "RESOLVED":
        return "해결 처리된 피드백이 없습니다.";
      default:
        return "등록된 피드백이 없습니다.";
    }
  }, [status]);

  const handleResolve = async () => {
    if (!resolveTarget?.feedbackId) return;
    setIsResolving(true);
    try {
      await resolveFeedback(resolveTarget.feedbackId);
      showToast("success", "피드백을 해결 처리했습니다.");
      setResolveTarget(null);
      await loadFeedbacks(status, page);
    } catch (err) {
      const message = err instanceof Error ? err.message : "피드백 해결 처리에 실패했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setIsResolving(false);
    }
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Feedback Management</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">피드백 관리</h1>
        <p className="mt-2 text-sm text-slate-500">사용자 피드백을 확인하고 해결 상태로 전환하세요.</p>
      </header>

      <Card
        title="피드백 목록"
        description="상태 필터를 전환해 미해결/해결됨 피드백을 확인할 수 있습니다."
      >
        <Tabs
          defaultValue={statusTabs[0].value}
          value={status}
          onValueChange={(value) => {
            setStatus(value as FeedbackFilter);
            setPage(0);
          }}
        >
          <TabsList>
            {statusTabs.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        {error && <InlineError message={error} className="mt-4" />}

        <div className="mt-4 rounded-2xl border border-slate-100">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>상태</TableHead>
                <TableHead>작성자</TableHead>
                <TableHead>이메일</TableHead>
                <TableHead>전화번호</TableHead>
                <TableHead>내용</TableHead>
                <TableHead>작성일</TableHead>
                <TableHead>해결일</TableHead>
                <TableHead className="text-right">액션</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!loading && feedbacks.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8}>
                    <EmptyState message="표시할 피드백이 없습니다." description={emptyDescription} />
                  </TableCell>
                </TableRow>
              )}
              {feedbacks.map((feedback) => (
                <TableRow key={feedback.feedbackId ?? feedback.content ?? Math.random().toString()}>
                  <TableCell>
                    <Badge variant={feedback.status === "RESOLVED" ? "success" : "secondary"}>
                      {feedback.status === "RESOLVED" ? "해결됨" : "미해결"}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-semibold text-slate-900">
                    {feedback.writer?.name ?? "-"}
                  </TableCell>
                  <TableCell>{feedback.writer?.email ?? "-"}</TableCell>
                  <TableCell>{feedback.writer?.phoneNumber ?? "-"}</TableCell>
                  <TableCell className="max-w-[240px] text-sm text-slate-600">
                    <p className="line-clamp-2">{feedback.content ?? "-"}</p>
                  </TableCell>
                  <TableCell>{formatDate(feedback.createdAt)}</TableCell>
                  <TableCell>{formatDate(feedback.resolvedAt)}</TableCell>
                  <TableCell className="text-right">
                    {feedback.status === "SUBMITTED" && feedback.feedbackId && (
                      <Button
                        variant="secondary"
                        className="h-9 px-3 text-xs"
                        onClick={() => setResolveTarget(feedback)}
                        disabled={loading}
                      >
                        해결 처리
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <ConfirmDialog
        open={Boolean(resolveTarget)}
        onClose={() => setResolveTarget(null)}
        onConfirm={() => void handleResolve()}
        title="피드백 해결 처리"
        message="선택한 피드백을 해결 상태로 변경할까요?"
        confirmText="해결 처리"
        cancelText="취소"
        isLoading={isResolving}
      />
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
