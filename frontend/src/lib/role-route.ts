import type { Route } from "next";

const ROLE_DASHBOARD_ROUTE = {
  SUPERADMIN: "/dashboard/superadmin",
  TEACHER: "/dashboard/teacher",
  ASSISTANT: "/dashboard/assistant",
  STUDENT: "/dashboard/student"
} as const satisfies Record<string, Route>;

type RoleKey = keyof typeof ROLE_DASHBOARD_ROUTE;
type DashboardRoute = (typeof ROLE_DASHBOARD_ROUTE)[RoleKey];

function isRoleKey(role: string): role is RoleKey {
  return role in ROLE_DASHBOARD_ROUTE;
}

export function getDashboardRoute(role?: string | null): DashboardRoute | null {
  if (!role) {
    return null;
  }

  const normalizedRole = role.toUpperCase();
  if (!isRoleKey(normalizedRole)) {
    return null;
  }

  return ROLE_DASHBOARD_ROUTE[normalizedRole];
}
