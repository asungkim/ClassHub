"use client";

import { useEffect, useMemo, type JSX } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/session/session-provider";
import { getDashboardRoute } from "@/lib/role-route";

type RoleGuardResult = {
  canRender: boolean;
  fallback: JSX.Element | null;
};

export function useRoleGuard(requiredRole: string): RoleGuardResult {
  const router = useRouter();
  const { status, member, error } = useSession();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/");
      return;
    }

    if (status === "authenticated" && member?.role && member.role !== requiredRole) {
      const target = getDashboardRoute(member.role) ?? "/";
      router.replace(target);
    }
  }, [status, member?.role, requiredRole, router]);

  const fallback = useMemo(() => {
    if (status === "loading") {
      return (
        <div className="rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
          세션을 확인 중입니다...
        </div>
      );
    }

    if (error) {
      return (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
          세션 정보를 불러오지 못했습니다. 다시 로그인해주세요.
        </div>
      );
    }

    return (
      <div className="rounded-2xl border border-gray-100 bg-gray-50 px-4 py-3 text-sm text-gray-700">
        권한을 확인하는 중입니다...
      </div>
    );
  }, [status, error]);

  if (status === "authenticated" && member?.role === requiredRole) {
    return { canRender: true, fallback: null };
  }

  return { canRender: false, fallback };
}
