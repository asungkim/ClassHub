import "./globals.css";
import type { Metadata } from "next";
import { ReactNode } from "react";
import { Providers } from "./providers";
import { AppChrome } from "@/components/ui/app-chrome";
import { AppErrorBoundary } from "@/components/ui/app-error-boundary";

export const metadata: Metadata = {
  title: "ClassHub Console",
  description: "Internal demo console for ClassHub APIs"
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <body className="bg-slate-950 text-slate-50 antialiased">
        <Providers>
          <AppChrome>
            <AppErrorBoundary>{children}</AppErrorBoundary>
          </AppChrome>
        </Providers>
      </body>
    </html>
  );
}
