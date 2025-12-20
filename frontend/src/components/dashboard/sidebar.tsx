"use client";

import Link from "next/link";
import type { Route } from "next";
import { usePathname } from "next/navigation";
import clsx from "clsx";
import { useSession } from "@/components/session/session-provider";
import { getDashboardRoute } from "@/lib/routes";
import type { Role } from "@/lib/routes";

type MenuItem = {
  href: string;
  label: string;
  icon: string;
};

type DashboardSidebarProps = {
  onNavigate?: () => void;
};

const menuByRole: Record<Role, MenuItem[]> = {
  TEACHER: [
    { href: "/teacher", label: "대시보드", icon: "📊" },
    { href: "/teacher/courses", label: "수업 관리", icon: "📚" },
    { href: "/teacher/students", label: "학생 관리", icon: "👨‍🎓" },
    { href: "/teacher/companies", label: "학원 관리", icon: "🏢" },
    { href: "/teacher/assistants", label: "조교 관리", icon: "👥" },
    { href: "/teacher/clinics", label: "클리닉 관리", icon: "🩺" },
  ],
  ASSISTANT: [
    { href: "/assistant", label: "대시보드", icon: "📊" },
    { href: "/assistant/courses", label: "반 목록", icon: "📚" },
    { href: "/assistant/clinics", label: "클리닉 일정", icon: "🩺" },
    { href: "/assistant/worklogs", label: "근무 일지", icon: "📝" },
  ],
  STUDENT: [
    { href: "/student", label: "대시보드", icon: "📊" },
    { href: "/student/my-courses", label: "내 수업", icon: "🎓" },
    { href: "/student/course/search", label: "반 검색", icon: "🔍" },
    { href: "/student/calendar", label: "일정", icon: "📅" },
    { href: "/student/clinics", label: "클리닉", icon: "🩺" },
  ],
  SUPER_ADMIN: [
    { href: "/admin", label: "대시보드", icon: "📊" },
    { href: "/admin/courses", label: "반 관리", icon: "📚" },
    { href: "/admin/companies", label: "학원 검증", icon: "🏢" },
    { href: "/admin/branches", label: "지점 검증", icon: "🏪" },
  ],
};

export function DashboardSidebar({ onNavigate }: DashboardSidebarProps) {
  const pathname = usePathname();
  const { member, logout } = useSession();
  const role = member?.role as Role | undefined;
  const menu = role ? menuByRole[role] : [];
  const dashboardHref = (role ? getDashboardRoute(role) : "/") as Route;
  const initials = member?.name?.[0] ?? "게";

  return (
    <div className="flex h-full flex-col border-r border-slate-200/80 bg-white/95 backdrop-blur">
      {/* 브랜드 */}
      <div className="flex items-center gap-3 border-b border-slate-200 px-6 py-5">
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-600 text-base font-semibold text-white shadow-md">
          CH
        </div>
        <div>
          <Link href={dashboardHref} className="text-sm font-semibold text-slate-900">
            ClassHub
          </Link>
          <p className="text-xs text-slate-500">{roleToLabel(role)}</p>
        </div>
      </div>

      {/* 사용자 카드 */}
      <div className="mx-5 mt-4 rounded-2xl border border-slate-200/70 bg-slate-50 px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-lg font-semibold text-white shadow">
            {initials}
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-900">{member?.name ?? "게스트"}</p>
            <p className="text-xs text-slate-500">{member?.email ?? "-"}</p>
          </div>
        </div>
      </div>

      {/* 메뉴 */}
      <nav className="mt-4 flex-1 space-y-1 overflow-y-auto px-4 pb-6">
        {menu.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.href}
              href={item.href as Route}
              onClick={onNavigate}
              className={clsx(
                "flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-semibold transition",
                isActive
                  ? "bg-blue-50 text-blue-700 shadow-[0_4px_12px_rgba(59,130,246,0.12)]"
                  : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
              )}
            >
              <span className="text-lg" aria-hidden>
                {item.icon}
              </span>
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="space-y-3 border-t border-slate-200 px-6 py-4 text-xs text-slate-500">
        <button
          type="button"
          onClick={() => void logout()}
          className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50"
        >
          로그아웃
        </button>
        <p className="text-center">필요한 메뉴는 좌측에서 선택하세요.</p>
      </div>
    </div>
  );
}

function roleToLabel(role?: Role) {
  switch (role) {
    case "TEACHER":
      return "선생님";
    case "ASSISTANT":
      return "조교";
    case "STUDENT":
      return "학생";
    case "SUPER_ADMIN":
      return "슈퍼어드민";
    default:
      return "게스트";
  }
}
