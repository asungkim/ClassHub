"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";

export default function TeacherClinicsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return <TeacherClinicsContent />;
}

function TeacherClinicsContent() {
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="클리닉 관리" description="좌측 메뉴에서 항목을 선택해 이동하세요.">
        <EmptyState message="클리닉 관리 홈입니다." description="지점별/주차별/출석부 메뉴로 이동합니다." />
      </Card>
    </div>
  );
}
