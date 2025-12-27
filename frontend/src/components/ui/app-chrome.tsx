"use client";

import { ReactNode, useCallback, useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import { Toaster } from "sonner";
import { NavigationBar } from "@/components/ui/navigation-bar";
import { Footer } from "@/components/ui/footer";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { useToast } from "@/components/ui/toast";
import { useSession } from "@/components/session/session-provider";
import { createFeedback } from "@/lib/dashboard-api";
import type { FeedbackCreateRequest } from "@/types/dashboard";

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
  const { status } = useSession();
  const { showToast } = useToast();
  const pathname = usePathname();
  const isHome = pathname === "/";
  const dashboardPrefixes = ["/dashboard", "/teacher", "/assistant", "/student", "/admin"];
  const isDashboard = dashboardPrefixes.some((prefix) => pathname?.startsWith(prefix));
  const isAuthRegister = pathname?.startsWith("/auth/register");
  const toaster = <Toaster position="top-center" richColors closeButton />;
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [feedbackContent, setFeedbackContent] = useState("");
  const [feedbackError, setFeedbackError] = useState<string | null>(null);
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
  const contentLength = feedbackContent.length;
  const trimmedContent = useMemo(() => feedbackContent.trim(), [feedbackContent]);
  const isOverLimit = contentLength > 2000;
  const canSubmitFeedback = trimmedContent.length > 0 && !isOverLimit && !feedbackSubmitting;

  const handleFeedbackSubmit = useCallback(async () => {
    if (!canSubmitFeedback) {
      if (!trimmedContent) {
        setFeedbackError("내용을 입력해 주세요.");
      }
      return;
    }
    setFeedbackSubmitting(true);
    setFeedbackError(null);
    const payload: FeedbackCreateRequest = { content: trimmedContent };
    try {
      await createFeedback(payload);
      showToast("success", "피드백이 등록되었습니다.");
      setFeedbackContent("");
      setFeedbackOpen(false);
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("feedback:created"));
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "피드백 등록에 실패했습니다.";
      setFeedbackError(message);
      showToast("error", message);
    } finally {
      setFeedbackSubmitting(false);
    }
  }, [canSubmitFeedback, showToast, trimmedContent]);

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
        {status === "authenticated" && (
          <>
            <button
              type="button"
              onClick={() => {
                setFeedbackError(null);
                setFeedbackOpen(true);
              }}
              className="fixed bottom-6 right-6 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-slate-900 text-2xl text-white shadow-lg transition hover:-translate-y-1 hover:bg-slate-800"
              aria-label="피드백 남기기"
            >
              ?
            </button>
            <Modal open={feedbackOpen} onClose={() => setFeedbackOpen(false)} title="건의사항 남기기" size="md">
              <div className="space-y-4">
                <p className="text-sm text-slate-500">
                  서비스 개선을 위해 의견을 남겨주세요. 제출된 내용은 관리자에게 전달됩니다.
                </p>
                <div className="space-y-2">
                  <textarea
                    value={feedbackContent}
                    onChange={(event) => {
                      setFeedbackContent(event.target.value);
                      if (feedbackError) {
                        setFeedbackError(null);
                      }
                    }}
                    maxLength={2000}
                    rows={6}
                    className="w-full resize-none rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-4 focus:ring-slate-200"
                    placeholder="개선이 필요한 점이나 불편했던 부분을 자세히 적어주세요."
                    aria-label="피드백 내용"
                  />
                  <div className="flex items-center justify-between text-xs text-slate-400">
                    <span>{contentLength}/2000</span>
                    {isOverLimit && <span className="text-rose-500">최대 2000자까지 입력할 수 있어요.</span>}
                  </div>
                </div>
                {feedbackError && <InlineError message={feedbackError} />}
                <div className="flex justify-end gap-2">
                  <Button variant="ghost" onClick={() => setFeedbackOpen(false)} disabled={feedbackSubmitting}>
                    닫기
                  </Button>
                  <Button onClick={() => void handleFeedbackSubmit()} disabled={!canSubmitFeedback}>
                    {feedbackSubmitting ? "전송 중..." : "제출하기"}
                  </Button>
                </div>
              </div>
            </Modal>
          </>
        )}
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
