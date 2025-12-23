"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function TeacherClinicSlotsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="지점별 클리닉 (슬롯)" description="출강 지점별 클리닉 슬롯을 관리합니다.">
        <EmptyState message="슬롯 시간표 준비 중입니다." description="지점 선택 후 슬롯이 표시됩니다." />
      </Card>
    </div>
  );
}
