"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import clsx from "clsx";
import { useSession } from "@/components/session/session-provider";
import { InlineError } from "@/components/ui/inline-error";
import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type InvitationRegisterRequest = components["schemas"]["InvitationRegisterRequest"];
type LoginResponse = components["schemas"]["LoginResponse"];

function InvitedRegisterContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { status, member, setToken } = useSession();
  const [form, setForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    name: "",
    termsAccepted: false
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successEmail, setSuccessEmail] = useState<string | null>(null);

  const code = searchParams.get("code");
  const isAuthenticated = status === "authenticated";
  const isSubmitDisabled =
    !form.email ||
    !form.password ||
    !form.confirmPassword ||
    !form.name ||
    form.password !== form.confirmPassword ||
    !form.termsAccepted ||
    isSubmitting ||
    !code;

  // 세션 스토리지에서 역할 정보 읽기 (검증 페이지에서 저장)
  const [inviteeRole, setInviteeRole] = useState<string | null>(null);
  const [isNameReadonly, setIsNameReadonly] = useState(false);

  useEffect(() => {
    const storedRole = sessionStorage.getItem("inviteeRole");
    setInviteeRole(storedRole);

    // 학생인 경우 studentName을 읽어와서 이름 필드에 미리 채우기
    if (storedRole === "STUDENT") {
      const studentName = sessionStorage.getItem("studentName");
      if (studentName) {
        setForm((prev) => ({ ...prev, name: studentName }));
        setIsNameReadonly(true);
      }
    }
  }, []);

  useEffect(() => {
    if (!code) {
      setErrorMessage("초대 코드가 없습니다. 초대 링크를 다시 확인해주세요.");
    }
  }, [code]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    if (isSubmitDisabled) return;

    try {
      setIsSubmitting(true);
      const payload: InvitationRegisterRequest = {
        code: code!,
        email: form.email,
        password: form.password,
        name: form.name
      };
      const response = await api.POST("/api/v1/auth/register/invited", { body: payload });

      if (response.error || !response.data?.data) {
        throw new Error(getApiErrorMessage(response.error, "회원가입 중 문제가 발생했습니다."));
      }

      const responseData: LoginResponse = response.data.data;
      setSuccessEmail(form.email);

      // SessionProvider에 토큰 설정 (자동으로 /me 호출하여 역할 정보 확보)
      await setToken(responseData.accessToken ?? null);

      // 세션 스토리지 정리
      sessionStorage.removeItem("inviteeRole");
      sessionStorage.removeItem("studentName");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "회원가입에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  useEffect(() => {
    if (successEmail && member?.role) {
      const timeout = setTimeout(() => {
        // 역할에 따라 대시보드 리디렉트
        if (member.role === "ASSISTANT") {
          router.push("/dashboard/assistant");
        } else if (member.role === "STUDENT") {
          router.push("/dashboard/student");
        } else {
          router.push("/");
        }
      }, 1500);
      return () => clearTimeout(timeout);
    }
  }, [successEmail, member, router]);

  if (isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4">
        <div className="rounded-3xl bg-white/90 px-8 py-10 text-center shadow-2xl ring-1 ring-white/60 backdrop-blur">
          <p className="text-sm uppercase tracking-wide text-indigo-600">이미 로그인됨</p>
          <h1 className="mt-4 text-2xl font-semibold text-gray-900">안녕하세요, {member?.name ?? "사용자"}님!</h1>
          <p className="mt-2 text-gray-600">이미 계정이 있으므로, 대시보드로 이동합니다.</p>
          <button
            className="mt-6 rounded-lg bg-indigo-600 px-6 py-2 text-white shadow-lg transition hover:bg-indigo-700"
            onClick={() => router.push("/")}
          >
            홈으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  const roleLabel = inviteeRole === "ASSISTANT" ? "조교" : inviteeRole === "STUDENT" ? "학생" : "";
  const badgeText = inviteeRole === "ASSISTANT" ? "Assistant 전용" : inviteeRole === "STUDENT" ? "Student 전용" : "초대 회원가입";
  const titleText =
    inviteeRole === "ASSISTANT"
      ? "ClassHub와 함께 조교 업무를 시작하세요."
      : inviteeRole === "STUDENT"
        ? "ClassHub와 함께 학습을 시작하세요."
        : "ClassHub에 가입하세요.";
  const descriptionText =
    inviteeRole === "ASSISTANT"
      ? "조교 계정을 생성하면 선생님을 도와 학생을 관리할 수 있습니다."
      : inviteeRole === "STUDENT"
        ? "학생 계정을 생성하면 나만의 대시보드에서 수업 내용을 확인할 수 있습니다."
        : "초대 링크를 통해 계정을 생성하세요.";
  const features =
    inviteeRole === "ASSISTANT"
      ? ["학생 초대 및 관리", "학생 정보 조회", "수업 기록 조회"]
      : inviteeRole === "STUDENT"
        ? ["내 수업 기록 조회", "클리닉 일정 확인", "선생님과 소통"]
        : ["대시보드 접근", "수업 관리", "커뮤니케이션"];
  const formTitle = roleLabel ? `${roleLabel} 회원가입` : "회원가입";
  const submitButtonText = roleLabel ? `${roleLabel} 계정 만들기` : "계정 만들기";

  return (
    <div className="relative isolate flex min-h-screen items-center justify-center overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16 text-gray-900">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-30 blur-3xl" />
      </div>

      <div className="relative z-10 flex w-full max-w-5xl flex-col items-center gap-12 sm:gap-16 lg:flex-row lg:items-start">
        <section className="mx-auto max-w-xl space-y-6 text-center md:text-left">
          <div className="inline-flex items-center rounded-full border border-white/70 bg-white/50 px-4 py-1 text-sm font-medium text-indigo-700 shadow-sm">
            {badgeText}
          </div>
          <h1 className="text-pretty text-4xl font-bold leading-tight text-gray-900 sm:text-5xl">{titleText}</h1>
          <p className="text-lg text-gray-600">{descriptionText}</p>
          <ul className="space-y-3 text-left text-gray-700">
            {features.map((feature) => (
              <li key={feature}>• {feature}</li>
            ))}
          </ul>
        </section>

        <div className="w-full max-w-lg rounded-3xl bg-white/90 p-8 shadow-2xl ring-1 ring-white/60 backdrop-blur">
          {successEmail ? (
            <RedirectPanel email={successEmail} />
          ) : (
            <form className="space-y-5" onSubmit={handleSubmit}>
              <div className="text-center">
                <h2 className="text-3xl font-semibold text-gray-900">{formTitle}</h2>
                <p className="mt-2 text-sm text-gray-500">ClassHub 대시보드를 사용하기 위한 계정을 만듭니다.</p>
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">이메일</label>
                <input
                  type="email"
                  value={form.email}
                  onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
                  placeholder="user@classhub.com"
                  required
                  className="w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 outline-none transition focus:border-transparent focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">비밀번호</label>
                <input
                  type="password"
                  value={form.password}
                  onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
                  placeholder="영문+숫자+특수문자 8자 이상"
                  required
                  className="w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 outline-none transition focus:border-transparent focus:ring-2 focus:ring-blue-500"
                />
                <PasswordHint password={form.password} />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">비밀번호 확인</label>
                <input
                  type="password"
                  value={form.confirmPassword}
                  onChange={(event) => setForm((prev) => ({ ...prev, confirmPassword: event.target.value }))}
                  required
                  className="w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 outline-none transition focus:border-transparent focus:ring-2 focus:ring-blue-500"
                />
                {form.confirmPassword && form.password !== form.confirmPassword ? (
                  <InlineError message="비밀번호가 일치하지 않습니다." className="mt-2" />
                ) : null}
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">이름</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
                  placeholder="홍길동"
                  required
                  readOnly={isNameReadonly}
                  className={clsx(
                    "w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 outline-none transition focus:border-transparent focus:ring-2 focus:ring-blue-500",
                    isNameReadonly && "cursor-not-allowed bg-gray-100"
                  )}
                />
                {isNameReadonly && (
                  <p className="mt-2 text-xs text-gray-500">학생 이름이 자동으로 입력되었습니다.</p>
                )}
              </div>

              <label className="flex cursor-pointer items-start gap-3 rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={form.termsAccepted}
                  onChange={(event) => setForm((prev) => ({ ...prev, termsAccepted: event.target.checked }))}
                  className="mt-1 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  required
                />
                <span>
                  <strong>서비스 이용약관</strong>과 <strong>개인정보 처리방침</strong>에 동의합니다.
                </span>
              </label>

              {errorMessage ? <InlineError message={errorMessage} /> : null}

              <button
                type="submit"
                disabled={isSubmitDisabled}
                className={clsx(
                  "w-full rounded-lg bg-gradient-to-r from-blue-600 to-purple-600 py-3 text-white shadow-lg transition hover:from-blue-700 hover:to-purple-700",
                  isSubmitDisabled && "cursor-not-allowed opacity-60"
                )}
              >
                {isSubmitting ? "가입 중..." : submitButtonText}
              </button>

              <p className="text-center text-sm text-gray-500">
                이미 계정이 있으신가요?{" "}
                <button type="button" className="font-semibold text-indigo-600 hover:text-indigo-700" onClick={() => router.push("/")}>
                  로그인하기
                </button>
              </p>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

function PasswordHint({ password }: { password: string }) {
  const rules = [
    { label: "8자 이상", valid: password.length >= 8 },
    { label: "영문/숫자/특수문자 포함", valid: /[A-Za-z]/.test(password) && /\d/.test(password) && /[^A-Za-z0-9]/.test(password) }
  ];

  return (
    <ul className="mt-2 text-xs text-gray-500">
      {rules.map((rule) => (
        <li key={rule.label} className={clsx("flex items-center gap-2", rule.valid ? "text-emerald-600" : "text-gray-400")}>
          <span className={clsx("h-2 w-2 rounded-full", rule.valid ? "bg-emerald-500" : "bg-gray-300")} />
          {rule.label}
        </li>
      ))}
    </ul>
  );
}

function RedirectPanel({ email }: { email: string }) {
  return (
    <div className="space-y-4 text-center">
      <h3 className="text-2xl font-semibold text-gray-900">대시보드로 이동합니다</h3>
      <p className="text-sm text-gray-500">{email} 계정으로 가입이 완료되었습니다. 잠시 후 대시보드로 이동합니다.</p>
    </div>
  );
}

export default function InvitedRegisterPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">
          <div className="rounded-3xl bg-white/90 px-8 py-10 text-center shadow-2xl ring-1 ring-white/60 backdrop-blur">
            <p className="text-gray-600">로딩 중...</p>
          </div>
        </div>
      }
    >
      <InvitedRegisterContent />
    </Suspense>
  );
}
