"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function StudentClinicsPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return <StudentClinicsContent />;
}

function StudentClinicsContent() {
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="클리닉" description="좌측 메뉴에서 항목을 선택해 이동하세요.">
        <EmptyState message="클리닉 홈입니다." description="시간표/이번 주 클리닉 메뉴로 이동합니다." />
      </Card>
    </div>
  );
}
