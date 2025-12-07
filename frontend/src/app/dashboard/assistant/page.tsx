"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardSections } from "@/components/dashboard/dashboard-sections";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";

export default function AssistantDashboardPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) return fallback;

  return (
    <DashboardShell title="조교 대시보드" subtitle="초대·일정·클리닉을 좌측 메뉴에서 관리하세요.">
      <DashboardSections role="ASSISTANT" />
    </DashboardShell>
  );
}
