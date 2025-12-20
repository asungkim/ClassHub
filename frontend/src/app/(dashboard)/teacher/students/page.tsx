"use client";

import { StudentManagementView } from "@/components/dashboard/student-management";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function TeacherStudentsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return <StudentManagementView role="TEACHER" />;
}
