import "./globals.css";
import type { Metadata } from "next";
import { ReactNode } from "react";
import { Providers } from "./providers";
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

export const metadata: Metadata = {
  title: "ClassHub Console",
  description: "Internal demo console for ClassHub APIs"
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <body className="bg-slate-950 text-slate-50 antialiased">
        <Providers>
          <div className="min-h-screen bg-gradient-to-br from-[#e8eaff] via-[#f5e6ff] to-[#ffe6f5]">
            <NavigationBar navItems={navItems} ctaLabel="대시보드 열기" ctaHref="#" />
            <main className="mx-auto max-w-5xl px-6 py-10">{children}</main>
            <Footer sections={footerSections} />
          </div>
        </Providers>
      </body>
    </html>
  );
}
