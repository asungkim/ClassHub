"use client";

import clsx from "clsx";
import { useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useFeedbackSummary } from "@/hooks/feedback/use-feedback-summary";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { TeacherLessonComposeModal } from "@/components/dashboard/progress/teacher-lesson-compose-modal";
import type { FeedbackResponse, FeedbackStatus } from "@/types/dashboard";

const highlightCards = [
  { label: "활성 반", value: "5개", sub: "이번 주 수업", tone: "from-blue-500 to-indigo-500" },
  { label: "승인 대기", value: "3건", sub: "학생 등록 요청", tone: "from-amber-500 to-orange-500" },
  { label: "조교 배치", value: "2명", sub: "이번 주 클리닉 담당", tone: "from-emerald-500 to-teal-500" },
  { label: "미확인 공지", value: "1건", sub: "어제 작성한 공지", tone: "from-purple-500 to-pink-500" }
];

const quickActions = [
  { title: "반 생성", description: "새 수업 반을 만들고 시간표를 등록합니다." },
  { title: "조교 초대", description: "조교를 초대해 클리닉/학생 관리를 맡길 수 있습니다." },
  { title: "학생 등록", description: "학생 수강 신청을 승인하거나 직접 추가합니다." },
  { title: "클리닉 설정", description: "클리닉 슬롯을 만들고 기본 담당자를 지정합니다." }
];

export default function TeacherDashboardPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const [composeOpen, setComposeOpen] = useState(false);
  const feedbackSummary = useFeedbackSummary();
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">Teacher Dashboard</p>
            <h1 className="mt-2 text-3xl font-bold text-slate-900">반과 학생을 한눈에 관리하세요.</h1>
            <p className="mt-2 text-sm text-slate-500">
              진행 중인 반, 초대 상태, 조교 배치 현황을 간단히 확인하고 필요한 액션을 바로 수행할 수 있습니다.
            </p>
          </div>
          <Button onClick={() => setComposeOpen(true)}>+ 수업 내용 작성</Button>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2">
        {highlightCards.map((stat) => (
          <div
            key={stat.label}
            className={clsx("rounded-3xl p-5 text-white shadow-lg", `bg-gradient-to-br ${stat.tone}`)}
          >
            <p className="text-xs font-semibold uppercase tracking-wide opacity-80">{stat.label}</p>
            <p className="mt-3 text-3xl font-bold">{stat.value}</p>
            <p className="text-sm opacity-90">{stat.sub}</p>
          </div>
        ))}
      </section>

      <FeedbackSummarySection summary={feedbackSummary} />

      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
        <h2 className="text-lg font-semibold text-slate-900">빠른 작업</h2>
        <p className="mt-1 text-sm text-slate-500">자주 사용하는 메뉴를 한곳에 모았습니다.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {quickActions.map((action) => (
            <button
              key={action.title}
              type="button"
              className="rounded-2xl border border-slate-200 px-4 py-4 text-left text-sm font-semibold text-slate-700 transition hover:border-blue-200 hover:bg-blue-50/50"
            >
              <p className="text-base text-slate-900">{action.title}</p>
              <p className="mt-1 text-xs font-normal text-slate-500">{action.description}</p>
            </button>
          ))}
        </div>
      </section>

      <TeacherLessonComposeModal open={composeOpen} onClose={() => setComposeOpen(false)} />
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
