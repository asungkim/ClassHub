"use client";

import { MemberProfileView } from "@/components/dashboard/member-profile";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function AssistantProfilePage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  if (!canRender) {
    return fallback;
  }
  return <MemberProfileView />;
}
