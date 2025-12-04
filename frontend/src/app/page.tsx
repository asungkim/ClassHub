"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import clsx from "clsx";
import { api } from "@/lib/api";
import { useSession } from "@/components/session/session-provider";
import { InlineError } from "@/components/ui/inline-error";

export default function HomePage() {
  const router = useRouter();
  const { status, member, error: sessionError, setToken } = useSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const handleLogin = async () => {
    setFormError(null);

    if (!email || !password) {
      setFormError("이메일과 비밀번호를 모두 입력해주세요.");
      return;
    }

    try {
      setIsLoading(true);
      const response = await api.POST("/auth/login", {
        body: { email, password }
      });

      if (!response.data?.data?.accessToken || response.error) {
        throw new Error(response.error?.message ?? "로그인에 실패했어요. 입력값을 다시 확인해주세요.");
      }

      setToken(response.data.data.accessToken);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "로그인 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const sessionMessage =
    status === "authenticated"
      ? `${member?.name ?? "사용자"}님, 다시 만나서 반가워요!`
      : status === "loading"
        ? "세션 상태를 확인하는 중입니다..."
        : "ClassHub에 로그인하고 대시보드를 살펴보세요.";

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16 text-gray-900">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-30 blur-3xl" />
      </div>

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col items-center gap-12 lg:flex-row lg:items-start">
        <section className="max-w-xl text-center lg:text-left">
          <div className="inline-flex items-center rounded-full border border-white/70 bg-white/50 px-4 py-1 text-sm font-medium text-indigo-700 shadow-sm">
            ClassHub
          </div>
          <h1 className="mt-6 text-4xl font-bold leading-tight text-gray-900 sm:text-5xl">
            학원의 초대, 레슨, 학생 관리를 <span className="text-indigo-600">한곳에서</span>.
          </h1>
          <p className="mt-4 text-lg text-gray-600">
            Teacher는 초대와 레슨을 준비하고, Assistant/Student는 초대 링크를 통해 자연스럽게 합류하세요. ClassHub는 역할별
            대시보드로 업무를 단순화합니다.
          </p>

          <div className="mt-8 flex flex-col gap-4 text-left text-gray-700">
            {[
              "Teacher 대시보드: 초대/레슨/학생 리포트 관리",
              "Assistant 대시보드: 초대 승인 및 일정 조율",
              "Student 포털: 개인 레슨 일정과 피드백 확인"
            ].map((item) => (
              <div key={item} className="flex items-start gap-3">
                <span className="mt-1 h-2 w-2 rounded-full bg-indigo-500" />
                <p>{item}</p>
              </div>
            ))}
          </div>
        </section>

        <div className="relative w-full max-w-md rounded-3xl bg-white/80 p-8 shadow-2xl ring-1 ring-white/60 backdrop-blur">
          <div className="mb-8 text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
              <ClassHubIcon />
            </div>
            <h2 className="text-3xl font-bold text-gray-900">ClassHub</h2>
            <p className="text-sm text-gray-500">교습소 운영을 위한 통합 대시보드</p>
          </div>

          <SessionBanner status={status} message={sessionMessage} error={sessionError} memberName={member?.name ?? ""} />

          <div className="mt-6 space-y-5">
            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-700">이메일</label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4 text-gray-400">
                  <MailIcon className="h-5 w-5" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="teacher@classhub.com"
                  className="w-full rounded-lg border border-gray-300 py-3 pl-12 pr-4 text-gray-900 outline-none transition focus:border-transparent focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-700">비밀번호</label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4 text-gray-400">
                  <LockIcon className="h-5 w-5" />
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      void handleLogin();
                    }
                  }}
                  className="w-full rounded-lg border border-gray-300 py-3 pl-12 pr-12 text-gray-900 outline-none transition focus:border-transparent focus:ring-2 focus:ring-blue-500"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute inset-y-0 right-0 flex items-center pr-4 text-gray-400 transition hover:text-gray-600"
                >
                  {showPassword ? <EyeOffIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between text-sm text-gray-600">
              <label className="flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(event) => setRememberMe(event.target.checked)}
                  className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                />
                <span className="ml-2">로그인 상태 유지</span>
              </label>
              <button type="button" className="font-medium text-blue-600 hover:text-blue-700">
                비밀번호 찾기
              </button>
            </div>

            {formError ? <InlineError message={formError} /> : null}

            <button
              onClick={() => void handleLogin()}
              disabled={isLoading || status === "loading"}
              className={clsx(
                "flex w-full items-center justify-center rounded-lg bg-gradient-to-r from-blue-600 to-purple-600 py-3 text-white shadow-lg transition hover:from-blue-700 hover:to-purple-700",
                (isLoading || status === "loading") && "cursor-not-allowed opacity-70"
              )}
            >
              {isLoading ? (
                <>
                  <SpinnerIcon className="mr-3 h-5 w-5 text-white" />
                  로그인 중...
                </>
              ) : (
                "로그인"
              )}
            </button>

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200" />
              </div>
              <div className="relative flex justify-center text-sm text-gray-500">
                <span className="bg-white px-4">또는</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => router.push("/auth/register/teacher")}
              className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-blue-600 py-3 font-semibold text-blue-600 transition hover:bg-blue-50"
            >
              <UsersIcon className="h-5 w-5" />
              선생님 회원가입
            </button>
          </div>

          <p className="mt-8 text-center text-sm text-gray-500">© 2025 ClassHub. All rights reserved.</p>
        </div>
      </div>

      <style jsx>{`
        @keyframes blob {
          0% {
            transform: translate(0px, 0px) scale(1);
          }
          33% {
            transform: translate(30px, -50px) scale(1.1);
          }
          66% {
            transform: translate(-20px, 20px) scale(0.9);
          }
          100% {
            transform: translate(0px, 0px) scale(1);
          }
        }
        .animate-blob {
          animation: blob 8s infinite;
        }
        .animation-delay-2000 {
          animation-delay: 2s;
        }
        .animation-delay-4000 {
          animation-delay: 4s;
        }
      `}</style>
    </div>
  );
}

function ClassHubIcon() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="h-8 w-8">
      <rect x="4" y="4" width="40" height="40" rx="8" className="fill-white/20" />
      <path
        d="M14 18h20M14 24h12M14 30h20"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function MailIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <rect x="3" y="5" width="18" height="14" rx="2" ry="2" />
      <path d="m3 7 9 6 9-6" />
    </svg>
  );
}

function LockIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}

function EyeIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOffIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-11-7-11-7a18.45 18.45 0 0 1 5.06-5.94" />
      <path d="M1 1 23 23" />
      <path d="M9.53 9.53a3 3 0 0 0 4.95 3.35" />
      <path d="M14.47 9.53a3 3 0 0 1 1.06 3.24" />
    </svg>
  );
}

function UsersIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <circle cx="9" cy="7" r="4" />
      <path d="M17 11a4 4 0 1 0-4-4" />
      <path d="M2 21v-2a4 4 0 0 1 4-4h6" />
      <path d="M21 21v-2a4 4 0 0 0-3-3.87" />
    </svg>
  );
}

function SpinnerIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.37 0 0 5.37 0 12h4Z" />
    </svg>
  );
}

type SessionBannerProps = {
  status: "loading" | "authenticated" | "unauthenticated";
  message: string;
  error: unknown;
  memberName: string;
};

function SessionBanner({ status, message, error, memberName }: SessionBannerProps) {
  if (status === "loading") {
    return (
      <div className="rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
        {message}
      </div>
    );
  }

  if (status === "authenticated") {
    return (
      <div className="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
        <strong>{memberName || "Teacher"}</strong> 계정으로 로그인되어 있습니다.
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
        세션을 확인하는 중 문제가 발생했습니다. 다시 로그인해주세요.
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-gray-100 bg-gray-50 px-4 py-3 text-sm text-gray-700">
      {message}
    </div>
  );
}
