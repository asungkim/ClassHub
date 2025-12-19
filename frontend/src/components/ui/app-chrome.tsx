"use client";

import { ReactNode } from "react";
import { usePathname } from "next/navigation";
import { Toaster } from "sonner";
import { NavigationBar } from "@/components/ui/navigation-bar";
import { Footer } from "@/components/ui/footer";

const navItems = [
  { label: "홈", href: "/" },
  { label: "컴포넌트", href: "/components" },
  { label: "문서", href: "https://nextjs.org/docs" }
];

const footerSections = [
  {
    title: "제품",
    links: [
      { label: "초대 관리", href: "#" },
      { label: "레슨 캘린더", href: "#" },
      { label: "학생 리포트", href: "#" }
    ]
  },
  {
    title: "리소스",
    links: [
      { label: "가이드 문서", href: "#" },
      { label: "API 참조", href: "#" },
      { label: "디자인 토큰", href: "#" }
    ]
  },
  {
    title: "지원",
    links: [
      { label: "이메일 문의", href: "mailto:support@classhub.dev" },
      { label: "상태 페이지", href: "#" },
      { label: "피드백 남기기", href: "#" }
    ]
  }
];

export function AppChrome({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const isHome = pathname === "/";
  const dashboardPrefixes = ["/dashboard", "/teacher", "/assistant", "/student", "/admin"];
  const isDashboard = dashboardPrefixes.some((prefix) => pathname?.startsWith(prefix));
  const isAuthRegister = pathname?.startsWith("/auth/register");
  const toaster = <Toaster position="top-center" richColors closeButton />;

  if (isHome || isAuthRegister) {
    return (
      <>
        {children}
        {toaster}
      </>
    );
  }

  if (isDashboard) {
    return (
      <div className="min-h-screen bg-slate-50 text-slate-900">
        <main className="min-h-screen">{children}</main>
        <div
          id="notification-layer"
          className="pointer-events-none fixed inset-x-0 top-16 z-40 flex justify-center px-4"
          aria-live="assertive"
        >
          <div className="w-full max-w-lg space-y-3" />
        </div>
        <div id="portal-toast-root" className="pointer-events-none fixed inset-0 z-50" />
        <div id="portal-modal-root" className="pointer-events-none fixed inset-0 z-50" />
        {toaster}
      </div>
    );
  }

  return (
    <div className="relative flex min-h-screen flex-col bg-gradient-to-br from-[#e8eaff] via-[#f5e6ff] to-[#ffe6f5]">
      <NavigationBar navItems={navItems} ctaLabel="대시보드 열기" ctaHref="#" />
      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-6 py-10">{children}</main>
      <Footer sections={footerSections} />

      <div
        id="notification-layer"
        className="pointer-events-none fixed inset-x-0 top-16 z-40 flex justify-center px-4"
        aria-live="assertive"
      >
        <div className="w-full max-w-lg space-y-3" />
      </div>
      <div id="portal-toast-root" className="pointer-events-none fixed inset-0 z-50" />
      <div id="portal-modal-root" className="pointer-events-none fixed inset-0 z-50" />
      {toaster}
    </div>
  );
}
