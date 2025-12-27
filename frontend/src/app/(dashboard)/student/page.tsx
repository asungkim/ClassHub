"use client";

import clsx from "clsx";
import { useMemo } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useFeedbackSummary } from "@/hooks/feedback/use-feedback-summary";
import { Badge } from "@/components/ui/badge";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import type { FeedbackResponse, FeedbackStatus } from "@/types/dashboard";

const stats = [
  { label: "수강 중인 반", value: "2개", sub: "이번 학기", tone: "from-sky-500 to-blue-500" },
  { label: "다가오는 일정", value: "3건", sub: "이번 주 수업/클리닉", tone: "from-indigo-500 to-purple-500" },
  { label: "과제 남음", value: "1건", sub: "금요일까지 제출", tone: "from-pink-500 to-rose-500" },
  { label: "선생님 공지", value: "2건", sub: "어제 등록된 메시지", tone: "from-teal-500 to-emerald-500" }
];

const actions = [
  { title: "수업 일정 보기", description: "오늘과 이번 주의 수업 일정을 확인합니다." },
  { title: "클리닉 신청", description: "필요한 클리닉 세션을 신청하거나 변경합니다." },
  { title: "과제 제출", description: "완료한 과제를 업로드하거나 제출 확인을 요청합니다." },
  { title: "공지/메시지 확인", description: "선생님과 조교가 보낸 알림을 확인합니다." }
];

export default function StudentDashboardPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  const feedbackSummary = useFeedbackSummary();
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-sky-500">Student Dashboard</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">내 수업과 과제를 한 번에 확인하세요.</h1>
        <p className="mt-2 text-sm text-slate-500">
          수강 중인 반, 제출해야 할 과제, 예정된 클리닉을 한 화면에서 볼 수 있습니다.
        </p>
      </section>

      <section className="grid gap-4 sm:grid-cols-2">
        {stats.map((stat) => (
          <div key={stat.label} className={clsx("rounded-3xl p-5 text-white shadow-lg", `bg-gradient-to-br ${stat.tone}`)}>
            <p className="text-xs font-semibold uppercase tracking-wide opacity-80">{stat.label}</p>
            <p className="mt-3 text-3xl font-bold">{stat.value}</p>
            <p className="text-sm opacity-90">{stat.sub}</p>
          </div>
        ))}
      </section>

      <FeedbackSummarySection summary={feedbackSummary} />

      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
        <h2 className="text-lg font-semibold text-slate-900">빠른 링크</h2>
        <p className="mt-1 text-sm text-slate-500">지금 필요한 작업을 선택해 진행하세요.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {actions.map((action) => (
            <button
              key={action.title}
              type="button"
              className="rounded-2xl border border-slate-200 px-4 py-4 text-left text-sm font-semibold text-slate-700 transition hover:border-sky-200 hover:bg-sky-50/50"
            >
              <p className="text-base text-slate-900">{action.title}</p>
              <p className="mt-1 text-xs font-normal text-slate-500">{action.description}</p>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

type FeedbackSummarySectionProps = {
  summary: {
    items: FeedbackResponse[];
    isLoading: boolean;
    error: string | null;
  };
};

function FeedbackSummarySection({ summary }: FeedbackSummarySectionProps) {
  const { items, isLoading, error } = summary;
  const visibleItems = useMemo(() => items.slice(0, 3), [items]);
  if (!isLoading && !error && visibleItems.length === 0) {
    return null;
  }

  return (
    <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">내 피드백</h2>
          <p className="mt-1 text-sm text-slate-500">최근 제출한 피드백 상태를 확인하세요.</p>
        </div>
      </div>

      {error && <InlineError message={error} className="mt-4" />}

      <div className="mt-4 space-y-3">
        {isLoading && (
          <>
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
          </>
        )}
        {!isLoading &&
          visibleItems.map((feedback, index) => {
            const status = (feedback.status ?? "SUBMITTED") as FeedbackStatus;
            const statusLabel = status === "RESOLVED" ? "해결됨" : "미해결";
            const badgeVariant = status === "RESOLVED" ? "success" : "secondary";
            return (
              <div
                key={feedback.feedbackId ?? `${feedback.createdAt ?? "feedback"}-${index}`}
                className="rounded-2xl border border-slate-200 px-4 py-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <Badge variant={badgeVariant}>{statusLabel}</Badge>
                  <span className="text-xs text-slate-500">작성일: {formatDate(feedback.createdAt)}</span>
                </div>
                <p className="mt-2 text-sm text-slate-600 line-clamp-2">{feedback.content ?? "-"}</p>
                {feedback.resolvedAt && (
                  <p className="mt-2 text-xs text-slate-400">해결일: {formatDate(feedback.resolvedAt)}</p>
                )}
              </div>
            );
          })}
      </div>
    </section>
  );
}

function formatDate(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("ko", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
