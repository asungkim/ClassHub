"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function TeacherClinicSessionsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  const weekRange = getCurrentWeekRange();

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card
        title="주차별 클리닉 (세션)"
        description={`이번 주 ${formatWeekRange(weekRange.start, weekRange.end)} 기준으로 세션을 표시합니다.`}
      >
        <EmptyState message="주간 세션 시간표 준비 중입니다." description="세션 목록과 긴급 세션 버튼을 제공합니다." />
      </Card>
    </div>
  );
}

function getCurrentWeekRange(baseDate: Date = new Date()) {
  const base = new Date(baseDate);
  const day = base.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  const start = new Date(base);
  start.setDate(base.getDate() + diffToMonday);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(start.getDate() + 6);
  end.setHours(23, 59, 59, 999);
  return { start, end };
}

function formatWeekRange(start: Date, end: Date) {
  const formatter = new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit"
  });
  return `${formatter.format(start)} ~ ${formatter.format(end)}`;
}
