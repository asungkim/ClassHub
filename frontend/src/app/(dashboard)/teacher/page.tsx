"use client";

import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";

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
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">Teacher Dashboard</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">반과 학생을 한눈에 관리하세요.</h1>
        <p className="mt-2 text-sm text-slate-500">
          진행 중인 반, 초대 상태, 조교 배치 현황을 간단히 확인하고 필요한 액션을 바로 수행할 수 있습니다.
        </p>
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
    </div>
  );
}
