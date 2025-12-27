"use client";

import { MemberProfileView } from "@/components/dashboard/member-profile";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function TeacherProfilePage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return <MemberProfileView />;
}
