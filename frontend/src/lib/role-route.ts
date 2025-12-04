const ROLE_DASHBOARD_ROUTE: Record<string, string> = {
  SUPERADMIN: "/dashboard/superadmin",
  TEACHER: "/dashboard/teacher",
  ASSISTANT: "/dashboard/assistant",
  STUDENT: "/dashboard/student"
};

export function getDashboardRoute(role?: string | null) {
  if (!role) {
    return null;
  }
  return ROLE_DASHBOARD_ROUTE[role] ?? null;
}
