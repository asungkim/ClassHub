"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardSections } from "@/components/dashboard/dashboard-sections";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";

export default function TeacherDashboardPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) return fallback;

  return (
    <DashboardShell title="선생님 대시보드" subtitle="수업과 클리닉 현황을 한눈에 확인하세요.">
      <DashboardSections role="TEACHER" />
    </DashboardShell>
  );
}
