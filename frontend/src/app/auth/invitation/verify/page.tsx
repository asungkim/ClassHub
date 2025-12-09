"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import clsx from "clsx";
import { useSession } from "@/components/session/session-provider";
import { InlineError } from "@/components/ui/inline-error";
import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type InvitationVerifyRequest = components["schemas"]["InvitationVerifyRequest"];
type InvitationVerifyResponse = components["schemas"]["InvitationVerifyResponse"];

function InvitationVerifyContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { status, member } = useSession();
  const [verifyData, setVerifyData] = useState<InvitationVerifyResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const code = searchParams.get("code");
  const isAuthenticated = status === "authenticated";

  useEffect(() => {
    if (isAuthenticated) {
      router.push("/");
      return;
    }

    if (!code) {
      setErrorMessage("초대 코드가 없습니다.");
      setIsLoading(false);
      return;
    }

    async function verifyInvitation() {
      try {
        setIsLoading(true);
        const requestBody: InvitationVerifyRequest = { code: code! };
        const response = await api.POST("/api/v1/auth/invitations/verify", { body: requestBody });

        if (response.error || !response.data?.data) {
          throw new Error(getApiErrorMessage(response.error, "초대 코드를 확인할 수 없습니다."));
        }

        setVerifyData(response.data.data);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "초대 코드 검증에 실패했습니다.");
      } finally {
        setIsLoading(false);
      }
    }

    void verifyInvitation();
  }, [code, isAuthenticated, router]);

  function handleConfirm() {
    if (!code || !verifyData) return;

    // 세션 스토리지에 inviteeRole 저장 (회원가입 페이지에서 UI 분기용)
    sessionStorage.setItem("inviteeRole", verifyData.inviteeRole ?? "");

    // 학생인 경우 studentProfile.name도 저장 (회원가입 페이지에서 이름 필드 미리 채우기)
    if (verifyData.inviteeRole === "STUDENT" && verifyData.studentProfile?.name) {
      sessionStorage.setItem("studentName", verifyData.studentProfile.name);
    }

    router.push(`/auth/register/invited?code=${code}`);
  }

  function handleCancel() {
    router.push("/");
  }

  if (isAuthenticated) {
    return null; // 리디렉트 중
  }

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">
        <div className="rounded-3xl bg-white/90 px-8 py-10 text-center shadow-2xl ring-1 ring-white/60 backdrop-blur">
          <p className="text-gray-600">초대 코드를 확인하는 중입니다...</p>
        </div>
      </div>
    );
  }

  if (errorMessage || !verifyData) {
    return (
      <div className="relative isolate flex min-h-screen items-center justify-center overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16">
        <div className="pointer-events-none absolute inset-0">
          <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-30 blur-3xl" />
          <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-30 blur-3xl" />
          <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-30 blur-3xl" />
        </div>

        <div className="relative z-10 w-full max-w-lg rounded-3xl bg-white/90 px-8 py-10 shadow-2xl ring-1 ring-white/60 backdrop-blur">
          <div className="space-y-6 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
              <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <h2 className="text-2xl font-semibold text-gray-900">초대 코드를 확인할 수 없습니다</h2>
            <p className="text-gray-600">{errorMessage || "초대 링크가 만료되었거나 잘못된 주소입니다."}</p>
            <button
              className="w-full rounded-lg bg-gradient-to-r from-blue-600 to-purple-600 py-3 text-white shadow-lg transition hover:from-blue-700 hover:to-purple-700"
              onClick={handleCancel}
            >
              홈으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    );
  }

  const isStudent = verifyData.inviteeRole === "STUDENT";
  const roleLabel = isStudent ? "학생 (Student)" : "조교 (Assistant)";
  const expiresAt = verifyData.expiresAt ? new Date(verifyData.expiresAt).toLocaleString("ko-KR") : "알 수 없음";

  return (
    <div className="relative isolate flex min-h-screen items-center justify-center overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-30 blur-3xl" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-30 blur-3xl" />
      </div>

      <div className="relative z-10 w-full max-w-lg rounded-3xl bg-white/90 px-8 py-10 shadow-2xl ring-1 ring-white/60 backdrop-blur">
        <div className="space-y-6">
          <div className="text-center">
            <h1 className="text-3xl font-semibold text-gray-900">초대 확인</h1>
            <p className="mt-2 text-sm text-gray-500">초대 정보를 확인하고 회원가입을 진행하세요.</p>
          </div>

          <div className="space-y-4 rounded-2xl border border-gray-200 bg-gray-50 p-6">
            <div>
              <p className="text-sm font-semibold text-gray-600">선생님</p>
              <p className="mt-1 text-lg font-medium text-gray-900">{verifyData.inviterName}</p>
            </div>

            <div>
              <p className="text-sm font-semibold text-gray-600">역할</p>
              <p className="mt-1 text-lg font-medium text-gray-900">{roleLabel}</p>
            </div>

            <div>
              <p className="text-sm font-semibold text-gray-600">만료일</p>
              <p className="mt-1 text-lg font-medium text-gray-900">{expiresAt}</p>
            </div>

            {isStudent && verifyData.studentProfile?.name && (
              <div className="mt-6 rounded-lg border-l-4 border-amber-500 bg-amber-50 p-4">
                <div className="mb-2">
                  <p className="text-sm font-semibold text-amber-800">학생 정보</p>
                  <p className="mt-1 text-base font-medium text-amber-900">이름: {verifyData.studentProfile.name}</p>
                </div>
                <p className="text-xs text-amber-700">⚠️ 위 정보가 본인 이름과 일치하는지 확인해주세요. </p>
                <p className="text-xs text-amber-700">⚠️ 정보가 다르면 선생님 혹은 조교에게 문의해주세요. </p>
              </div>
            )}
          </div>

          <div className="flex gap-3">
            <button
              type="button"
              onClick={handleCancel}
              className="flex-1 rounded-lg border border-gray-300 bg-white py-3 text-gray-700 transition hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              className="flex-1 rounded-lg bg-gradient-to-r from-blue-600 to-purple-600 py-3 text-white shadow-lg transition hover:from-blue-700 hover:to-purple-700"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function InvitationVerifyPage() {
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
      <InvitationVerifyContent />
    </Suspense>
  );
}
