"use client";

import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";

const stats = [
  { label: "검증 대기", value: "4건", sub: "Company/Branch 요청", tone: "from-indigo-600 to-blue-600" },
  { label: "등록된 회사", value: "12곳", sub: "최근 30일 기준", tone: "from-emerald-500 to-teal-500" },
  { label: "미확인 지점", value: "2곳", sub: "현장 확인 필요", tone: "from-amber-500 to-orange-500" },
  { label: "지원 티켓", value: "1건", sub: "답변 대기 중", tone: "from-gray-600 to-slate-700" }
];

const actions = [
  { title: "Company 검증", description: "새로 등록된 학원의 정보를 확인하고 승인합니다." },
  { title: "Branch 상태 변경", description: "지점의 verifiedStatus를 검토 후 업데이트합니다." },
  { title: "계정 비활성화", description: "이용 중지 요청이 들어온 계정을 처리합니다." },
  { title: "운영 리포트 다운로드", description: "등록/검증 현황 리포트를 생성합니다." }
];

export default function AdminDashboardPage() {
  const { canRender, fallback } = useRoleGuard("SUPER_ADMIN");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Super Admin Dashboard</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">학원/지점 검증과 운영 상태를 모니터링하세요.</h1>
        <p className="mt-2 text-sm text-slate-500">
          신규 등록 요청, 지점 검증 상태, 운영 지원 티켓을 한곳에서 관리합니다.
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
        <h2 className="text-lg font-semibold text-slate-900">운영 작업</h2>
        <p className="mt-1 text-sm text-slate-500">승인/검증 관련 작업을 빠르게 처리하세요.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {actions.map((action) => (
            <button
              key={action.title}
              type="button"
              className="rounded-2xl border border-slate-200 px-4 py-4 text-left text-sm font-semibold text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50/50"
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
