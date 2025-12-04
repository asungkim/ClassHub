import Link from "next/link";
import { describeEnv } from "@/lib/env";

export default function HomePage() {
  const env = describeEnv();

  return (
    <section className="space-y-6">
      <div>
        <p className="text-sm uppercase tracking-wide text-slate-500">Getting started</p>
        <h1 className="text-3xl font-semibold text-slate-900">ClassHub 프런트엔드 부트스트랩</h1>
        <p className="mt-2 max-w-2xl text-slate-600">
          Next.js 16 + Tailwind CSS 4 조합으로 구축된 최소한의 스캐폴딩입니다. 환경 변수를 설정하고 공용
          API 클라이언트를 연결한 뒤 Auth · Invitation · StudentProfile 흐름을 여기에 확장하세요.
        </p>
      </div>

      <div className="rounded-2xl border border-white/60 bg-white/80 px-6 py-5 shadow">
        <h2 className="text-lg font-semibold text-slate-900">환경 변수</h2>
        <p className="mt-2 text-sm text-slate-500">
          `.env.local` 파일을 생성해{" "}
          <code className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-700">NEXT_PUBLIC_API_BASE_URL</code> 과
          필요시 <code className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-700">NEXT_PUBLIC_MOCK_TOKEN</code>{" "}
          을 지정하세요. 샘플 값은 `.env.local.example`을 참고하면 됩니다.
        </p>
        <div className="mt-4 text-xs text-slate-500">
          현재 베이스 URL: <span className="font-mono text-slate-800">{env.apiBaseUrl}</span>
        </div>
      </div>

      <div className="rounded-2xl border border-white/60 bg-white/80 px-6 py-5 shadow">
        <h2 className="text-lg font-semibold text-slate-900">다음 단계</h2>
        <ol className="list-decimal space-y-2 pl-5 text-slate-600">
          <li>`npm run openapi` 로 타입을 생성하고 `src/lib/api.ts`를 사용해 호출합니다.</li>
          <li>
            <Link href="/components" className="font-semibold text-primary hover:text-primary/80">
              /components
            </Link>{" "}
            페이지에서 공통 UI 토큰과 컴포넌트를 참고합니다.
          </li>
          <li>Auth/Invitation · StudentProfile 흐름을 `src/app` 아래에 확장하고 React Query 활용.</li>
        </ol>
      </div>
    </section>
  );
}
