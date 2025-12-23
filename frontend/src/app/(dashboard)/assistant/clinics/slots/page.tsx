"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function AssistantClinicSlotsPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="선생님별 클리닉 (슬롯)" description="담당 선생님 기준 슬롯을 조회합니다.">
        <EmptyState message="슬롯 시간표 준비 중입니다." description="선생님/지점 선택 후 슬롯이 표시됩니다." />
      </Card>
    </div>
  );
}
