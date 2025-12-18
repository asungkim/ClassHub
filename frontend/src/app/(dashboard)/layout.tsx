"use client";

import { useEffect, useState } from "react";
import clsx from "clsx";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/session/session-provider";
import { DashboardSidebar } from "@/components/dashboard/sidebar";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { status, member } = useSession();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // 인증되지 않은 사용자는 홈으로 리다이렉트
  useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/");
    }
  }, [status, router]);

  // 로딩 중이거나 인증되지 않은 경우 스켈레톤 표시
  if (status === "loading" || status === "unauthenticated") {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-50">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-slate-50 text-slate-900">
      {/* 사이드바 (모바일 토글 + 데스크톱 고정) */}
      <aside
        className={clsx(
          "fixed inset-y-0 left-0 z-30 w-72 bg-white/95 shadow-xl transition-transform duration-200 lg:static lg:translate-x-0 lg:shadow-none",
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <DashboardSidebar onNavigate={() => setSidebarOpen(false)} />
      </aside>
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/30 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
          role="presentation"
        />
      )}
      {!sidebarOpen && (
        <button
          type="button"
          onClick={() => setSidebarOpen(true)}
          className="fixed bottom-5 left-4 z-20 inline-flex items-center rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-md lg:hidden"
        >
          메뉴
        </button>
      )}

      {/* 메인 영역 */}
      <div className="flex min-h-screen flex-1 flex-col">
        <main className="flex-1 overflow-y-auto bg-slate-50/60">
          <div className="mx-auto w-full max-w-6xl px-4 py-6 sm:px-6 lg:px-10 lg:py-10">{children}</div>
        </main>

        <footer className="border-t border-slate-200 bg-white/80 px-4 py-2 text-center text-xs text-slate-500 lg:px-8">
          © {new Date().getFullYear()} ClassHub. 필요한 메뉴는 좌측 사이드바에서 선택하세요.
        </footer>
      </div>
    </div>
  );
}
