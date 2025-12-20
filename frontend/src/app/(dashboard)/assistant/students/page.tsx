"use client";

import { StudentManagementView } from "@/components/dashboard/student-management";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function AssistantStudentsPage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }
  return <StudentManagementView role="ASSISTANT" />;
}
