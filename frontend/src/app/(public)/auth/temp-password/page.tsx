"use client";

import { useEffect, useState } from "react";
import type { Route } from "next";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useSession } from "@/components/session/session-provider";
import { Card } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { SectionHeading } from "@/components/ui/section-heading";
import { formatPhoneNumber, validatePhoneNumber } from "@/lib/format-phone";
import { getDashboardRoute } from "@/lib/routes";
import type { components } from "@/types/openapi";

type TempPasswordRequest = components["schemas"]["TempPasswordRequest"];
type TempPasswordResponse = components["schemas"]["TempPasswordResponse"];

export default function TempPasswordPage() {
  const router = useRouter();
  const { status, member } = useSession();
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [tempPassword, setTempPassword] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (status === "authenticated" && member?.role) {
      const dashboardRoute = getDashboardRoute(member.role) as Route;
      router.replace(dashboardRoute);
    }
  }, [status, member, router]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setTempPassword(null);

    const errors: Record<string, string> = {};
    if (!email) errors.email = "이메일을 입력해주세요.";
    if (!phoneNumber) errors.phoneNumber = "전화번호를 입력해주세요.";
    if (phoneNumber && !validatePhoneNumber(phoneNumber)) {
      errors.phoneNumber = "010으로 시작하는 유효한 전화번호를 입력해주세요.";
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    try {
      setIsSubmitting(true);
      const requestBody: TempPasswordRequest = {
        email,
        phoneNumber
      };
      const response = await api.POST("/api/v1/auth/temp-password", { body: requestBody });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "임시 비밀번호 발급에 실패했습니다."));
      }
      const data: TempPasswordResponse = response.data?.data ?? {};
      if (!data.tempPassword) {
        throw new Error("임시 비밀번호 발급에 실패했습니다.");
      }
      setTempPassword(data.tempPassword);
    } catch (error) {
      const message = error instanceof Error ? error.message : "임시 비밀번호 발급에 실패했습니다.";
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
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16 text-slate-900">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-64 w-64 rounded-full bg-blue-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-2000 absolute top-32 right-8 h-64 w-64 rounded-full bg-purple-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-72 w-72 rounded-full bg-pink-200 opacity-30 blur-3xl" />
      </div>

      <div className="relative z-10 mx-auto flex w-full max-w-4xl flex-col gap-8">
        <SectionHeading
          eyebrow="Password Reset"
          title="임시 비밀번호 발급"
          description="이메일과 휴대폰 번호로 본인 확인 후 임시 비밀번호를 발급합니다."
        />

        <Card title="본인 확인" description="가입한 이메일과 휴대폰 번호를 입력해주세요.">
          <form className="space-y-4" onSubmit={handleSubmit}>
            <TextField
              label="이메일"
              name="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              error={fieldErrors.email}
              required
            />
            <TextField
              label="휴대폰 번호"
              name="phoneNumber"
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(formatPhoneNumber(event.target.value))}
              error={fieldErrors.phoneNumber}
              helperText="숫자만 입력해주세요."
              required
            />

            {formError ? <InlineError message={formError} /> : null}

            <div className="flex justify-end">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "발급 중..." : "임시 비밀번호 발급"}
              </Button>
            </div>
          </form>
        </Card>

        {tempPassword ? (
          <Card title="임시 비밀번호 발급 완료" description="아래 비밀번호로 로그인 후 반드시 변경해주세요.">
            <div className="flex flex-col gap-4">
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-lg font-semibold text-amber-700">
                임시 비밀번호: {tempPassword}
              </div>
              <Button asChild>
                <Link href="/">로그인하러 가기</Link>
              </Button>
            </div>
          </Card>
        ) : null}

        <div className="text-center text-sm text-slate-600">
          이미 로그인 페이지로 돌아가려면{" "}
          <Link href="/" className="font-semibold text-blue-600 hover:text-blue-700">
            로그인 화면으로 이동
          </Link>
          하세요.
        </div>
      </div>
    </div>
  );
}
