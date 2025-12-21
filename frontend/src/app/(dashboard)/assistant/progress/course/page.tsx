"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { SectionHeading } from "@/components/ui/section-heading";
import { CourseProgressSection } from "@/components/dashboard/progress/course-progress-section";

export default function AssistantCourseProgressPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6">
      <SectionHeading
        eyebrow="Progress"
        title="반별 진도"
        description="담당 반의 공통 진도 기록을 최신순으로 확인합니다."
      />
      <CourseProgressSection role="ASSISTANT" />
    </div>
  );
}
