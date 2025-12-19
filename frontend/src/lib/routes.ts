/**
 * 역할별 대시보드 경로 유틸
 */

export type Role = "TEACHER" | "ASSISTANT" | "STUDENT" | "SUPER_ADMIN";

/**
 * 역할에 따른 메인 대시보드 경로 반환
 */
export function getDashboardRoute(role: Role | string): string {
  const routeMap: Record<string, string> = {
    TEACHER: "/teacher",
    ASSISTANT: "/assistant",
    STUDENT: "/student",
    SUPER_ADMIN: "/admin",
  };

  return routeMap[role] || "/";
}

/**
 * 현재 경로가 역할에 맞는 대시보드 경로인지 확인
 */
export function isValidDashboardRoute(pathname: string, role: Role | string): boolean {
  const dashboardRoute = getDashboardRoute(role);
  return pathname.startsWith(dashboardRoute);
}
