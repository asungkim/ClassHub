"use client";

import Link from "next/link";

export default function HomePage() {
  return (
    <section className="space-y-6">
      <div>
        <p className="text-sm uppercase tracking-wide text-slate-400">Getting started</p>
        <h1 className="text-3xl font-semibold text-white">ClassHub 프런트엔드 부트스트랩</h1>
        <p className="mt-2 max-w-2xl text-slate-300">
          Next.js 16 + Tailwind CSS 4 기반의 최소 구성을 제공합니다. 환경 변수와 API 클라이언트를
          설정한 후, Auth · Invitation · StudentProfile 화면을 여기에 추가하면 됩니다.
        </p>
      </div>

      <div className="rounded-xl border border-slate-800/80 bg-slate-900/40 px-6 py-5">
        <h2 className="text-lg font-semibold text-white">환경 변수</h2>
        <p className="mt-2 text-sm text-slate-300">
          `.env.local` 파일을 생성하고 <code className="rounded bg-slate-800 px-2 py-0.5 text-xs">
            NEXT_PUBLIC_API_BASE_URL
          </code>{" "}
          를 지정하세요. 샘플 값은 `.env.local.example` 에 정리되어 있습니다.
        </p>
      </div>

      <div className="rounded-xl border border-slate-800/80 bg-slate-900/40 px-6 py-5">
        <h2 className="text-lg font-semibold text-white">다음 단계</h2>
        <ol className="list-decimal space-y-2 pl-5 text-slate-300">
          <li>Auth/Invitation 화면을 `app` 디렉터리에 추가합니다.</li>
          <li>공용 API 클라이언트(`src/lib/apiClient.ts`)를 import 하여 데이터 호출을 구현합니다.</li>
          <li>StudentProfile/PersonalLesson 조회/등록 컴포넌트를 만든 뒤 Tailwind로 스타일링합니다.</li>
        </ol>
        <div className="mt-4 text-sm text-slate-400">
          문서:{" "}
          <Link
            href="https://nextjs.org/docs"
            className="text-brand-300 underline hover:text-brand-200"
            target="_blank"
          >
            Next.js Docs
          </Link>
        </div>
      </div>
    </section>
  );
}
