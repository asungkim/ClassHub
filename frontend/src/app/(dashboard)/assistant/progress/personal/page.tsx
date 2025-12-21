"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { SectionHeading } from "@/components/ui/section-heading";
import { PersonalProgressSection } from "@/components/dashboard/progress/personal-progress-section";

export default function AssistantPersonalProgressPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6">
      <SectionHeading
        eyebrow="Progress"
        title="개인 진도"
        description="담당 학생의 개인 진도 기록을 최신순으로 확인합니다."
      />
      <PersonalProgressSection role="ASSISTANT" />
    </div>
  );
}
