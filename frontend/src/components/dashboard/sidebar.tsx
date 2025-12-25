"use client";

import Link from "next/link";
import type { Route } from "next";
import { usePathname } from "next/navigation";
import clsx from "clsx";
import { useState } from "react";
import { useSession } from "@/components/session/session-provider";
import { getDashboardRoute } from "@/lib/routes";
import type { Role } from "@/lib/routes";

type MenuItem = {
  label: string;
  icon: string;
  href?: string;
  children?: MenuItem[];
};

type DashboardSidebarProps = {
  onNavigate?: () => void;
};

const menuByRole: Record<Role, MenuItem[]> = {
  TEACHER: [
    { href: "/teacher", label: "대시보드", icon: "📊" },
    { href: "/teacher/companies", label: "학원 관리", icon: "🏢" },
    { href: "/teacher/courses", label: "반 관리", icon: "📚" },
    { href: "/teacher/students", label: "학생 관리", icon: "👨‍🎓" },
    { href: "/teacher/assistants", label: "조교 관리", icon: "👥" },
    {
      label: "진도 관리",
      icon: "📈",
      children: [
        { href: "/teacher/progress/course", label: "반별 진도", icon: "📘" },
        { href: "/teacher/progress/personal", label: "개인 진도", icon: "🧑‍🎓" },
        { href: "/teacher/calendar", label: "학생별 캘린더", icon: "🗓️" }
      ]
    },
    {
      label: "클리닉 관리",
      icon: "🩺",
      children: [
        { href: "/teacher/clinics/slots", label: "지점별 클리닉", icon: "🧩" },
        { href: "/teacher/clinics/sessions", label: "주차별 클리닉", icon: "🗓️" },
        { href: "/teacher/clinics/attendance", label: "오늘의 출석부", icon: "🧾" }
      ]
    },
  ],
  ASSISTANT: [
    { href: "/assistant", label: "대시보드", icon: "📊" },
    { href: "/assistant/courses", label: "반 목록", icon: "📚" },
    { href: "/assistant/students", label: "학생 관리", icon: "👨‍🎓" },
    {
      label: "클리닉 일정",
      icon: "🩺",
      children: [
        { href: "/assistant/clinics/slots", label: "선생님별 클리닉", icon: "🧩" },
        { href: "/assistant/clinics/sessions", label: "주차별 클리닉", icon: "🗓️" },
        { href: "/assistant/clinics/attendance", label: "오늘의 출석부", icon: "🧾" }
      ]
    },
    { href: "/assistant/worklogs", label: "근무 일지", icon: "📝" },
    {
      label: "진도 관리",
      icon: "📈",
      children: [
        { href: "/assistant/progress/course", label: "반별 진도", icon: "📘" },
        { href: "/assistant/progress/personal", label: "개인 진도", icon: "🧑‍🎓" },
        { href: "/assistant/calendar", label: "학생별 캘린더", icon: "🗓️" }
      ]
    },
    
  ],
  STUDENT: [
    { href: "/student", label: "대시보드", icon: "📊" },
    { href: "/student/my-courses", label: "내 수업", icon: "🎓" },
    { href: "/student/teachers", label: "선생님 관리", icon: "🧑‍🏫" },
    {
      label: "클리닉",
      icon: "🩺",
      children: [
        { href: "/student/clinics/schedule", label: "클리닉 시간표", icon: "🧭" },
        { href: "/student/clinics/week", label: "이번 주 클리닉", icon: "🗓️" }
      ]
    }
  ],
  SUPER_ADMIN: [
    { href: "/admin", label: "대시보드", icon: "📊" },
    { href: "/admin/courses", label: "반 관리", icon: "📚" },
    { href: "/admin/student-enrollment-requests", label: "학생 요청 관리", icon: "📝" },
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
  const [expandedMenus, setExpandedMenus] = useState<Record<string, boolean>>({});

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
          if (item.children && item.children.length > 0) {
            const isChildActive = item.children.some((child) =>
              child.href ? pathname === child.href || pathname.startsWith(`${child.href}/`) : false
            );
            const isExpanded = expandedMenus[item.label] ?? isChildActive;
            return (
              <div key={item.label} className="space-y-1">
                <button
                  type="button"
                  onClick={() =>
                    setExpandedMenus((prev) => ({
                      ...prev,
                      [item.label]: !isExpanded
                    }))
                  }
                  className={clsx(
                    "flex w-full items-center justify-between rounded-2xl px-4 py-3 text-sm font-semibold transition",
                    isChildActive
                      ? "bg-blue-50 text-blue-700"
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                  )}
                >
                  <span className="flex items-center gap-3">
                    <span className="text-lg" aria-hidden>
                      {item.icon}
                    </span>
                    <span>{item.label}</span>
                  </span>
                  <span className="text-xs text-slate-400" aria-hidden>
                    {isExpanded ? "▲" : "▼"}
                  </span>
                </button>
                {isExpanded ? (
                  <div className="ml-6 space-y-1 border-l border-slate-200 pl-4">
                    {item.children.map((child) => {
                      const isActive = child.href
                        ? pathname === child.href || pathname.startsWith(`${child.href}/`)
                        : false;
                      return (
                        <Link
                          key={child.href}
                          href={child.href as Route}
                          onClick={onNavigate}
                          className={clsx(
                            "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold transition",
                            isActive
                              ? "bg-blue-50 text-blue-700"
                              : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                          )}
                        >
                          <span className="text-base" aria-hidden>
                            {child.icon}
                          </span>
                          <span>{child.label}</span>
                        </Link>
                      );
                    })}
                  </div>
                ) : null}
              </div>
            );
          }

          const isActive = item.href ? pathname === item.href || pathname.startsWith(`${item.href}/`) : false;
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
