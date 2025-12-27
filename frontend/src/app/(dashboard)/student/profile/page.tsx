"use client";

import { MemberProfileView } from "@/components/dashboard/member-profile";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function StudentProfilePage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }
  return <MemberProfileView />;
}
