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

type RegisterTeacherRequest = components["schemas"]["RegisterMemberRequest"];
type RegisterTeacherResponse = components["schemas"]["RsDataLoginResponse"];

export default function TeacherRegisterPage() {
  const router = useRouter();
  const { status, member, setToken } = useSession();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // 이미 로그인된 사용자는 즉시 메인 페이지로 리다이렉트
  useEffect(() => {
    if (status === "authenticated" && member?.role) {
      const dashboardRoute = getDashboardRoute(member.role) as Route;
      router.replace(dashboardRoute);
    }
  }, [status, member, router]);

  // 비밀번호 검증 (백엔드 스펙과 동일)
  const isPasswordValid = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-={}:;"'`~<>,.?/\\|\[\]]).{8,64}$/.test(password);
  const isPhoneValid = validatePhoneNumber(phone);
  const passwordsMatch = password === confirmPassword;
  const isFormValid = email && password && confirmPassword && name && phone && isPasswordValid && isPhoneValid && passwordsMatch;

  const handlePhoneInput = (value: string) => {
    setPhone(formatPhoneNumber(value));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFieldErrors({});

    // Validation
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
      // 첫 번째 에러 필드로 포커스 이동
      const firstErrorField = Object.keys(errors)[0];
      document.querySelector<HTMLInputElement>(`input[name="${firstErrorField}"]`)?.focus();
      return;
    }

    try {
      setIsSubmitting(true);

      const requestBody: RegisterTeacherRequest = {
        email,
        password,
        name,
        phoneNumber: phone
      };

      const response = await api.POST("/api/v1/members/register/teacher", {
        body: requestBody
      });

      if (response.error || !response.data?.data?.accessToken) {
        const message = getApiErrorMessage(response.error, "회원가입에 실패했습니다. 다시 시도해주세요.");

        // 중복 이메일 에러 처리
        if (message.includes("이메일") || message.includes("DUPLICATE")) {
          setFieldErrors({ email: "이미 가입된 이메일입니다." });
        } else {
          setFormError(message);
        }
        return;
      }

      const accessToken = response.data.data.accessToken;

      // 세션 복원 → SessionProvider가 role 확인 후 useEffect에서 대시보드 리다이렉트
      await setToken(accessToken);
    } catch (error) {
      const message = error instanceof Error ? error.message : "회원가입 중 오류가 발생했습니다.";
      setFormError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 로딩 중이거나 이미 로그인된 경우 스켈레톤 표시
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
      {/* 배경 애니메이션 */}
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-20 blur-3xl md:opacity-30" />
        <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-20 blur-3xl md:opacity-30" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-20 blur-3xl md:opacity-30" />
      </div>

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-8 lg:flex-row lg:items-center">
        {/* 좌측 Hero 영역 (Desktop만) */}
        <div className="hidden lg:block lg:flex-1">
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
                <ClassHubIcon />
              </div>
              <div>
                <h1 className="text-4xl font-bold text-gray-900">ClassHub</h1>
                <p className="text-sm text-gray-600">수업 관리의 새로운 기준</p>
              </div>
            </div>
            <div className="space-y-3 text-gray-700">
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>수업/학생/조교를 한곳에서 관리</span>
              </div>
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>클리닉 일정 자동화</span>
              </div>
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>학생 진도 실시간 추적</span>
              </div>
            </div>
          </div>
        </div>

        {/* 우측 회원가입 카드 */}
        <div className="w-full lg:flex-1">
          <div className="relative w-full max-w-md mx-auto rounded-3xl bg-white/80 p-8 shadow-2xl ring-1 ring-white/60 backdrop-blur">
            {/* 모바일용 헤더 */}
            <div className="mb-6 text-center lg:hidden">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
                <ClassHubIcon />
              </div>
              <h2 className="text-2xl font-bold text-gray-900">ClassHub</h2>
            </div>

            <div className="mb-6">
              <h2 className="text-2xl font-bold text-gray-900">선생님 회원가입</h2>
              <p className="mt-1 text-sm text-gray-600">ClassHub에서 수업을 시작하세요</p>
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
                onChange={(e) => setEmail(e.target.value)}
                placeholder="teacher@classhub.com"
                error={fieldErrors.email}
                required
                aria-describedby={fieldErrors.email ? "email-error" : undefined}
              />

              <TextField
                label="비밀번호"
                type="password"
                name="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                error={fieldErrors.password}
                required
                aria-describedby={fieldErrors.password ? "password-error" : undefined}
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
                  type="password"
                  name="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  error={fieldErrors.confirmPassword}
                  required
                  aria-describedby={fieldErrors.confirmPassword ? "confirm-password-error" : undefined}
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
                onChange={(e) => setName(e.target.value)}
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
                onChange={(e) => handlePhoneInput(e.target.value)}
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

function SpinnerIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.37 0 0 5.37 0 12h4Z" />
    </svg>
  );
}
