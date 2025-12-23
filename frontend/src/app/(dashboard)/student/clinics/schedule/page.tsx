"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function StudentClinicSchedulePage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="클리닉 시간표" description="기본 슬롯을 시간표로 확인하고 선택합니다.">
        <EmptyState message="기본 슬롯 시간표 준비 중입니다." description="선생님/지점 선택 후 시간표가 표시됩니다." />
      </Card>
    </div>
  );
}
