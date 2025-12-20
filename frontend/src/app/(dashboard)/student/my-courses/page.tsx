"use client";

import Link from "next/link";

import { EmptyState } from "@/components/shared/empty-state";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useRoleGuard } from "@/hooks/use-role-guard";

export default function StudentMyCoursesPage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">My Courses</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">내 수업</h1>
        <p className="mt-2 text-sm text-slate-500">
          승인된 반과 대기 중인 신청 상태를 이곳에서 확인할 수 있습니다. 아직 연결된 수업이 없다면 반 검색 페이지에서
          신청을 진행해 주세요.
        </p>
      </header>

      <Card title="연결된 수업" description="곧 Enrollment 승인 내역이 여기에 표시됩니다.">
        <EmptyState
          message="아직 연결된 수업이 없습니다."
          description="반 검색 페이지에서 수업을 신청하면, 승인 후 이곳에 표시됩니다."
        />
        <div className="mt-6 text-center">
          <Button asChild>
            <Link href="/student/courses">반 검색하러 가기</Link>
          </Button>
        </div>
      </Card>
    </div>
  );
}
