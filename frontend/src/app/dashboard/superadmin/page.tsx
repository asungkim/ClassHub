"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardSections } from "@/components/dashboard/dashboard-sections";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";

export default function SuperAdminDashboardPage() {
  const { canRender, fallback } = useRoleGuard("SUPERADMIN");
  if (!canRender) return fallback;

  return (
    <DashboardShell title="슈퍼어드민 대시보드" subtitle="역할 공통 대시보드 UI로 운영 현황을 확인하세요.">
      <DashboardSections role="SUPERADMIN" />
    </DashboardShell>
  );
}
