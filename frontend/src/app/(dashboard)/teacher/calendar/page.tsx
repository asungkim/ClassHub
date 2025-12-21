"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { StudentCalendarPage } from "@/components/dashboard/calendar/student-calendar-page";

export default function TeacherStudentCalendarPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }

  return <StudentCalendarPage role="TEACHER" />;
}
