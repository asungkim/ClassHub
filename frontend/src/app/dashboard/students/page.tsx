"use client";

import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function StudentsPagePlaceholder() {
  const { canRender, fallback } = useRoleGuard(["TEACHER", "ASSISTANT"]);
  if (!canRender) return fallback;

  return (
    <DashboardShell title="학생 관리" subtitle="학생 관리 화면은 다음 단계에서 구현됩니다.">
      <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center shadow-sm">
        <p className="text-lg font-semibold text-slate-900">학생 관리 UI 준비 중</p>
        <p className="mt-2 text-sm text-slate-600">다음 작업에서 목록/등록/수정/퇴원 기능을 연결합니다.</p>
      </div>
    </DashboardShell>
  );
}
