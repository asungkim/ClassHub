"use client";

import { ReactNode, useMemo, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { Route } from "next";
import clsx from "clsx";
import { Button } from "@/components/ui/button";
import { useSession } from "@/components/session/session-provider";
import { getDashboardRoute } from "@/lib/role-route";

type SidebarItem = {
  label: string;
  href?: Route;
  badge?: string;
  children?: SidebarItem[];
};

type DashboardShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function DashboardShell({ title, subtitle, children }: DashboardShellProps) {
  const { member, logout } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [invitationMenuOpen, setInvitationMenuOpen] = useState(false);
  const role = member?.role ?? "";
  const isTeacher = role === "TEACHER";
  const isAssistant = role === "ASSISTANT";

  const dashboardHref: Route =
    (getDashboardRoute(member?.role ?? "TEACHER") as Route | undefined) ?? "/dashboard/teacher";

  const sidebarItems: SidebarItem[] = useMemo(
    () => {
      const items: SidebarItem[] = [{ label: "대시보드", href: dashboardHref }];

      if (isTeacher) {
        items.push({ label: "조교 관리", href: "/dashboard/assistants" as Route });
      }

      if (isTeacher || isAssistant) {
        items.push({ label: "학생 관리", href: "/dashboard/students" as Route });
      }

      // 초대 관리 (접기/펼치기)
      const invitationChildren: SidebarItem[] = [];
      if (isTeacher) {
        invitationChildren.push({ label: "조교 초대", href: "/dashboard/invitations/assistant" as Route });
      }
      if (isTeacher || isAssistant) {
        invitationChildren.push({ label: "학생 초대", href: "/dashboard/invitations/student" as Route });
      }
      if (invitationChildren.length > 0) {
        items.push({ label: "초대 관리", children: invitationChildren });
      }

      items.push(
        { label: "수업 관리" },
        { label: "클리닉 현황" },
        { label: "공지사항" },
        { label: "메시지", badge: "5" }
      );

      return items;
    },
    [dashboardHref, isAssistant, isTeacher]
  );

  const handleLogout = async () => {
    await logout();
    router.replace("/");
  };

  const initials = member?.name?.[0] ?? "게";
  const roleLabel = member?.role ? roleToLabel(member.role) : "게스트";

  return (
    <div className="flex min-h-screen bg-slate-50 text-slate-900">
      <aside
        className={clsx(
          "fixed inset-y-0 left-0 z-30 w-72 bg-white shadow-xl transition-transform duration-200 md:translate-x-0",
          sidebarOpen ? "translate-x-0" : "-translate-x-full",
          "md:static md:block md:shadow-none md:ring-1 md:ring-slate-200/70"
        )}
      >
        <div className="flex h-full flex-col border-r border-slate-200/80">
          <div className="flex items-center gap-3 px-6 py-5">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 text-base font-semibold text-white shadow-md">
              CH
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">ClassHub</p>
              <p className="text-xs text-slate-500">운영 콘솔</p>
            </div>
          </div>

          <div className="mx-4 rounded-2xl bg-slate-50 px-4 py-3">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-lg font-semibold text-white shadow-md">
                {initials}
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-900">{member?.name ?? "게스트"}</p>
                <p className="text-xs text-slate-500">{roleLabel}</p>
              </div>
            </div>
          </div>

          <nav className="mt-4 flex-1 space-y-1 px-3">
            {sidebarItems.map((item) => {
              const isActive = item.href
                ? pathname === item.href || pathname?.startsWith(`${item.href}/`)
                : false;

              // 자식 메뉴가 있는 경우 (초대 관리)
              if (item.children) {
                const hasActiveChild = item.children.some(
                  (child) => child.href && (pathname === child.href || pathname?.startsWith(`${child.href}/`))
                );
                const isOpen = invitationMenuOpen || hasActiveChild;

                return (
                  <div key={item.label}>
                    <button
                      onClick={() => setInvitationMenuOpen(!invitationMenuOpen)}
                      className={clsx(
                        "flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-semibold transition",
                        hasActiveChild
                          ? "bg-blue-50 text-blue-700"
                          : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                      )}
                    >
                      <span>{item.label}</span>
                      <svg
                        className={clsx("h-4 w-4 transition-transform", isOpen && "rotate-180")}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                    {isOpen && (
                      <div className="mt-1 space-y-1 pl-4">
                        {item.children.map((child) => {
                          const isChildActive = child.href
                            ? pathname === child.href || pathname?.startsWith(`${child.href}/`)
                            : false;
                          return child.href ? (
                            <Link
                              key={child.label}
                              href={child.href}
                              className={clsx(
                                "flex items-center rounded-xl px-4 py-2 text-sm font-medium transition",
                                isChildActive
                                  ? "bg-blue-50 text-blue-700"
                                  : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                              )}
                            >
                              {child.label}
                            </Link>
                          ) : null;
                        })}
                      </div>
                    )}
                  </div>
                );
              }

              // 일반 메뉴
              return item.href ? (
                <Link
                  key={item.label}
                  href={item.href}
                  className={clsx(
                    "flex items-center justify-between rounded-xl px-4 py-3 text-sm font-semibold transition",
                    isActive
                      ? "bg-blue-50 text-blue-700 shadow-[0_1px_4px_rgba(59,130,246,0.15)]"
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                  )}
                >
                  <span>{item.label}</span>
                  {item.badge && (
                    <span className="flex h-6 min-w-[1.5rem] items-center justify-center rounded-full bg-rose-100 px-2 text-xs font-semibold text-rose-600">
                      {item.badge}
                    </span>
                  )}
                </Link>
              ) : (
                <div
                  key={item.label}
                  className="flex items-center justify-between rounded-xl px-4 py-3 text-sm font-semibold text-slate-400"
                  aria-disabled
                >
                  <span>{item.label}</span>
                  {item.badge && (
                    <span className="flex h-6 min-w-[1.5rem] items-center justify-center rounded-full bg-rose-100 px-2 text-xs font-semibold text-rose-600">
                      {item.badge}
                    </span>
                  )}
                </div>
              );
            })}
          </nav>

          <div className="mt-auto px-4 pb-6">
            <Button
              variant="secondary"
              onClick={handleLogout}
              className="w-full justify-center rounded-xl border-slate-200 bg-white text-sm font-semibold text-slate-700 shadow-sm hover:border-slate-300"
            >
              로그아웃
            </Button>
          </div>
        </div>
      </aside>

      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/30 backdrop-blur-sm md:hidden"
          onClick={() => setSidebarOpen(false)}
          role="presentation"
        />
      )}

      <div className="flex flex-1 flex-col md:pl-0 md:pt-0">
        <header className="flex items-center justify-between border-b border-slate-200/80 bg-white/80 px-4 py-4 backdrop-blur md:px-10">
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-slate-700 shadow-sm md:hidden"
              onClick={() => setSidebarOpen((prev) => !prev)}
            >
              Menu
            </Button>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Dashboard</p>
              <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
              {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
            </div>
          </div>
        </header>

        <div className="flex-1 px-4 pb-16 pt-6 md:px-10 md:pt-8">{children}</div>

        <footer className="border-t border-slate-200 bg-white/80 px-6 py-4 text-center text-xs text-slate-500 md:px-10">
          © {new Date().getFullYear()} ClassHub. 필요한 메뉴는 왼쪽 사이드바에서 선택하세요.
        </footer>
      </div>
    </div>
  );
}

function roleToLabel(role: string) {
  switch (role) {
    case "TEACHER":
      return "선생님";
    case "ASSISTANT":
      return "조교";
    case "STUDENT":
      return "학생";
    case "SUPERADMIN":
      return "슈퍼어드민";
    default:
      return "게스트";
  }
}
