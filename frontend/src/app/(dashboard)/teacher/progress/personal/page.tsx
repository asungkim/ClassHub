"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { SectionHeading } from "@/components/ui/section-heading";
import { PersonalProgressSection } from "@/components/dashboard/progress/personal-progress-section";

export default function TeacherPersonalProgressPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6">
      <SectionHeading
        eyebrow="Progress"
        title="개인 진도"
        description="학생별 개인 진도 기록을 최신순으로 확인합니다."
      />
      <PersonalProgressSection role="TEACHER" />
    </div>
  );
}
