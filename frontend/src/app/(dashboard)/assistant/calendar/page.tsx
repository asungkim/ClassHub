"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { StudentCalendarPage } from "@/components/dashboard/calendar/student-calendar-page";

export default function AssistantStudentCalendarPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }

  return <StudentCalendarPage role="ASSISTANT" />;
}
