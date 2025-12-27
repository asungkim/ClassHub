"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import type { Route } from "next";
import clsx from "clsx";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useSession } from "@/components/session/session-provider";
import { DashboardSidebar } from "@/components/dashboard/sidebar";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { getProfileRoute } from "@/lib/routes";

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

      <Suspense fallback={null}>
        <ForcePasswordPrompt role={member?.role} />
      </Suspense>
    </div>
  );
}

function ForcePasswordPrompt({ role }: { role?: string | null }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [forcePasswordOpen, setForcePasswordOpen] = useState(false);

  const forcePasswordChange = useMemo(() => searchParams.get("forcePasswordChange") === "1", [searchParams]);

  useEffect(() => {
    if (forcePasswordChange) {
      setForcePasswordOpen(true);
    }
  }, [forcePasswordChange]);

  return (
    <ConfirmDialog
      open={forcePasswordOpen}
      title="비밀번호 변경 안내"
      message="임시 비밀번호로 로그인했습니다. 내 정보에서 새 비밀번호로 변경해주세요."
      confirmText="변경하러 가기"
      cancelText="나중에"
      onClose={() => {
        setForcePasswordOpen(false);
        if (forcePasswordChange) {
          const nextParams = new URLSearchParams(searchParams.toString());
          nextParams.delete("forcePasswordChange");
          const nextPath = nextParams.toString() ? `${pathname}?${nextParams.toString()}` : pathname;
          router.replace(nextPath as Route);
        }
      }}
      onConfirm={() => {
        setForcePasswordOpen(false);
        const profileRoute = role ? getProfileRoute(role) : "/";
        router.replace(profileRoute as Route);
      }}
    />
  );
}
