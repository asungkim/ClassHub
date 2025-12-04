"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";

export default function SuperAdminDashboardPage() {
  const { canRender, fallback } = useRoleGuard("SUPERADMIN");
  if (!canRender) return fallback;

  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-bold text-slate-900">SuperAdmin Dashboard</h1>
      <p className="text-slate-600">도메인/시스템 전반을 관리할 기능이 여기에 추가될 예정입니다.</p>
    </div>
  );
}
