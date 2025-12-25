"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { SectionHeading } from "@/components/ui/section-heading";
import { CourseProgressSection } from "@/components/dashboard/progress/course-progress-section";

export default function TeacherCourseProgressPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6">
      <SectionHeading
        eyebrow="Progress"
        title="반별 진도"
        description="선택한 반의 진도 기록을 최신순으로 확인합니다."
      />
      <CourseProgressSection role="TEACHER" />
    </div>
  );
}
