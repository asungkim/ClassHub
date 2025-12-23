"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function AssistantClinicAttendancePage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }
  const todayLabel = formatTodayLabel();

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="오늘의 출석부" description={`${todayLabel} 기준 세션을 표시합니다.`}>
        <EmptyState message="출석부 준비 중입니다." description="세션 선택 후 출석 명단이 표시됩니다." />
      </Card>
    </div>
  );
}

function formatTodayLabel(date: Date = new Date()) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(date);
}
