"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";

export default function TeacherDashboardPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) return fallback;

  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-bold text-slate-900">Teacher Dashboard</h1>
      <p className="text-slate-600">초대 관리, 레슨 일정, 학생 리포트 위젯이 여기에 배치됩니다.</p>
    </div>
  );
}
