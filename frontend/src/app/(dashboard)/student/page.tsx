"use client";

import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";

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
