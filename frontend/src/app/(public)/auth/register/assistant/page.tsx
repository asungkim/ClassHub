"use client";

import { useState, useEffect } from "react";
import type { Route } from "next";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import { useSession } from "@/components/session/session-provider";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { PasswordRequirementList } from "@/components/ui/password-requirement-list";
import { formatPhoneNumber, validatePhoneNumber } from "@/lib/format-phone";
import { getDashboardRoute } from "@/lib/routes";
import type { components } from "@/types/openapi";

type RegisterAssistantRequest = components["schemas"]["RegisterMemberRequest"];

export default function AssistantRegisterPage() {
  const router = useRouter();
  const { status, member, setToken } = useSession();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (status === "authenticated" && member?.role) {
      const dashboardRoute = getDashboardRoute(member.role) as Route;
      router.replace(dashboardRoute);
    }
  }, [status, member, router]);

  const isPasswordValid = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-={}:;"'`~<>,.?/\\|\[\]]).{8,64}$/.test(password);
  const isPhoneValid = validatePhoneNumber(phone);
  const passwordsMatch = password === confirmPassword;
  const isFormValid = email && password && confirmPassword && name && phone && isPasswordValid && isPhoneValid && passwordsMatch;

  const handlePhoneInput = (value: string) => {
    setPhone(formatPhoneNumber(value));
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});

    const errors: Record<string, string> = {};
    if (!email) errors.email = "이메일을 입력해주세요.";
    if (!password) errors.password = "비밀번호를 입력해주세요.";
    if (!isPasswordValid) errors.password = "비밀번호는 영문, 숫자, 특수문자를 포함하고 8~64자여야 합니다.";
    if (!confirmPassword) errors.confirmPassword = "비밀번호 확인을 입력해주세요.";
    if (!passwordsMatch) errors.confirmPassword = "비밀번호가 일치하지 않습니다.";
    if (!name) errors.name = "이름을 입력해주세요.";
    if (!phone) errors.phone = "전화번호를 입력해주세요.";
    if (!isPhoneValid) errors.phone = "010으로 시작하는 유효한 전화번호를 입력해주세요.";

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      const firstErrorField = Object.keys(errors)[0];
      document.querySelector<HTMLInputElement>(`input[name="${firstErrorField}"]`)?.focus();
      return;
    }

    try {
      setIsSubmitting(true);

      const requestBody: RegisterAssistantRequest = {
        email,
        password,
        name,
        phoneNumber: phone
      };

      const response = await api.POST("/api/v1/members/register/assistant", {
        body: requestBody
      });

      if (response.error || !response.data?.data?.accessToken) {
        const message = getApiErrorMessage(response.error, "회원가입에 실패했습니다. 다시 시도해주세요.");
        if (message.includes("이메일") || message.includes("DUPLICATE")) {
          setFieldErrors({ email: "이미 가입된 이메일입니다." });
        } else if (message.includes("비활성화")) {
          setFormError("비활성화된 계정입니다. 선생님에게 문의하세요.");
        } else {
          setFormError(message);
        }
        return;
      }

      const accessToken = response.data.data.accessToken;
      await setToken(accessToken);
    } catch (error) {
      const message = error instanceof Error ? error.message : "회원가입 중 오류가 발생했습니다.";
      setFormError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (status === "loading" || status === "authenticated") {
    return (
      <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16">
        <div className="relative z-10 mx-auto flex w-full max-w-6xl items-center justify-center">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
        </div>
      </div>
    );
  }

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16 text-gray-900">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-20 blur-3xl md:opacity-30" />
        <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-20 blur-3xl md:opacity-30" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-20 blur-3xl md:opacity-30" />
      </div>

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-8 lg:flex-row lg:items-center">
        <div className="hidden lg:block lg:flex-1">
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
                <ClassHubIcon />
              </div>
              <div>
                <h1 className="text-4xl font-bold text-gray-900">ClassHub</h1>
                <p className="text-sm text-gray-600">강사와 함께 성장하는 조교 플랫폼</p>
              </div>
            </div>
            <div className="space-y-3 text-gray-700">
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>배정된 학생/클리닉 일정을 한눈에 확인</span>
              </div>
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>근무일지와 공지사항을 실시간 공유</span>
              </div>
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>선생님과 함께 학생 진도 관리</span>
              </div>
            </div>
          </div>
        </div>

        <div className="w-full lg:flex-1">
          <div className="relative mx-auto w-full max-w-md rounded-3xl bg-white/80 p-8 shadow-2xl ring-1 ring-white/60 backdrop-blur">
            <div className="mb-6 text-center lg:hidden">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
                <ClassHubIcon />
              </div>
              <h2 className="text-2xl font-bold text-gray-900">ClassHub</h2>
            </div>

            <div className="mb-6">
              <h2 className="text-2xl font-bold text-gray-900">조교 회원가입</h2>
              <p className="mt-1 text-sm text-gray-600">선생님과 연결되어 수업을 지원하세요</p>
            </div>

            {formError && (
              <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <TextField
                label="이메일"
                type="email"
                name="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="assistant@classhub.com"
                error={fieldErrors.email}
                required
                aria-describedby={fieldErrors.email ? "email-error" : undefined}
              />

              <TextField
                label="비밀번호"
                type={showPassword ? "text" : "password"}
                name="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="••••••••"
                error={fieldErrors.password}
                required
                aria-describedby={fieldErrors.password ? "password-error" : undefined}
                rightElement={
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="rounded-full p-1 text-slate-400 transition hover:text-slate-600"
                    aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
                  >
                    {showPassword ? <EyeOffIcon className="h-4 w-4" /> : <EyeIcon className="h-4 w-4" />}
                  </button>
                }
              />

              {password && (
                <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
                  <p className="mb-2 text-xs font-semibold text-slate-700">비밀번호 요구사항</p>
                  <PasswordRequirementList password={password} />
                </div>
              )}

              <div>
              <TextField
                label="비밀번호 확인"
                type={showConfirmPassword ? "text" : "password"}
                name="confirmPassword"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="••••••••"
                error={fieldErrors.confirmPassword}
                required
                aria-describedby={fieldErrors.confirmPassword ? "confirm-password-error" : undefined}
                rightElement={
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                    className="rounded-full p-1 text-slate-400 transition hover:text-slate-600"
                    aria-label={showConfirmPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
                  >
                    {showConfirmPassword ? <EyeOffIcon className="h-4 w-4" /> : <EyeIcon className="h-4 w-4" />}
                  </button>
                }
              />
              {confirmPassword && password && !passwordsMatch && (
                <p className="mt-2 text-xs font-semibold text-rose-600">비밀번호가 일치하지 않습니다.</p>
              )}
              {confirmPassword && password && passwordsMatch && (
                <div className="mt-2 flex items-center gap-2 text-xs text-emerald-600">
                  <CheckCircleIcon className="h-4 w-4" />
                  <span className="font-medium">비밀번호가 일치합니다</span>
                </div>
                )}
              </div>

              <TextField
                label="이름"
                type="text"
                name="name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="홍길동"
                error={fieldErrors.name}
                required
                aria-describedby={fieldErrors.name ? "name-error" : undefined}
              />

              <TextField
                label="전화번호"
                type="tel"
                name="phone"
                value={phone}
                onChange={(event) => handlePhoneInput(event.target.value)}
                placeholder="01012345678"
                helperText="숫자만 입력하세요"
                error={fieldErrors.phone}
                inputMode="numeric"
                pattern="[0-9-]*"
                maxLength={13}
                required
                aria-describedby={fieldErrors.phone ? "phone-error" : undefined}
              />

              <Button
                type="submit"
                disabled={!isFormValid || isSubmitting}
                className="w-full"
                leftIcon={isSubmitting ? <SpinnerIcon className="h-5 w-5 animate-spin" /> : undefined}
              >
                {isSubmitting ? "가입 중..." : "회원가입"}
              </Button>

              <p className="text-center text-sm text-gray-600">
                이미 계정이 있으신가요?{" "}
                <Link href="/" className="font-semibold text-blue-600 hover:text-blue-700">
                  로그인
                </Link>
              </p>
            </form>
          </div>
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

function CheckCircleIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <circle cx="12" cy="12" r="10" />
      <polyline points="9 12 11 14 15 10" />
    </svg>
  );
}

function EyeIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOffIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M3 5l18 14" />
      <path d="M10.8 10.8a3 3 0 0 0 4.2 4.2" />
      <path d="M9.5 5.5A9.9 9.9 0 0 1 12 5c6.5 0 10 7 10 7a18.7 18.7 0 0 1-4.1 4.9" />
      <path d="M6.3 6.3A18.7 18.7 0 0 0 2 12s3.5 6 10 6a9.9 9.9 0 0 0 5.1-1.4" />
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
