"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardSections } from "@/components/dashboard/dashboard-sections";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";

export default function StudentDashboardPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) return fallback;

  return (
    <DashboardShell title="학생 대시보드" subtitle="수업 일정과 피드백을 한 번에 확인하세요.">
      <DashboardSections role="STUDENT" />
    </DashboardShell>
  );
}
