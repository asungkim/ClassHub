import "./globals.css";
import type { Metadata } from "next";
import { ReactNode } from "react";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "ClassHub Console",
  description: "Internal demo console for ClassHub APIs"
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <body className="bg-slate-950 text-slate-50 antialiased">
        <Providers>
          <div className="min-h-screen">
            <header className="border-b border-slate-900/50 bg-slate-950/80 backdrop-blur">
              <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
                <div className="text-lg font-semibold text-white">ClassHub Demo</div>
                <nav className="text-sm text-slate-400">
                  <span>Next.js 16 · Tailwind 4</span>
                </nav>
              </div>
            </header>
            <main className="mx-auto max-w-5xl px-6 py-10">{children}</main>
          </div>
        </Providers>
      </body>
    </html>
  );
}
