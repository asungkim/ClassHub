"use client";

import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";

const stats = [
  { label: "담당 반", value: "3개", sub: "이번 주 배정", tone: "from-emerald-500 to-green-500" },
  { label: "클리닉 일정", value: "4건", sub: "오늘 처리해야 할 세션", tone: "from-blue-500 to-cyan-500" },
  { label: "미완료 기록", value: "2건", sub: "클리닉 피드백 작성", tone: "from-amber-500 to-orange-500" },
  { label: "근무 일지", value: "0건", sub: "오늘은 모두 제출 완료", tone: "from-purple-500 to-pink-500" }
];

const actions = [
  { title: "클리닉 체크인", description: "오늘 열리는 클리닉 세션 출석을 기록합니다." },
  { title: "학생 피드백 작성", description: "담당 학생에게 진행 상황을 남겨 선생님과 공유합니다." },
  { title: "초대 코드 확인", description: "학생 초대 링크를 확인하거나 재전송합니다." },
  { title: "근무 일지 작성", description: "오늘 근무 내용을 요약해 선생님에게 전달합니다." }
];

export default function AssistantDashboardPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-emerald-500">Assistant Dashboard</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">클리닉 일정과 학생 지원을 빠르게 확인하세요.</h1>
        <p className="mt-2 text-sm text-slate-500">
          담당 반, 학생 요청, 근무 일지를 한 화면에서 관리할 수 있습니다.
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

      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
        <h2 className="text-lg font-semibold text-slate-900">바로가기</h2>
        <p className="mt-1 text-sm text-slate-500">지금 처리해야 할 일을 빠르게 시작하세요.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {actions.map((action) => (
            <button
              key={action.title}
              type="button"
              className="rounded-2xl border border-slate-200 px-4 py-4 text-left text-sm font-semibold text-slate-700 transition hover:border-emerald-200 hover:bg-emerald-50/50"
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
