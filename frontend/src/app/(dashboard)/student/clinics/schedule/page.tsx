"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicContexts } from "@/hooks/clinic/use-clinic-contexts";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/shared/empty-state";

type ClinicContextCourse = {
  courseId: string;
  courseName: string;
  recordId?: string;
  defaultClinicSlotId?: string;
};

type ClinicContextGroup = {
  key: string;
  teacherId: string;
  branchId: string;
  teacherName: string;
  branchName: string;
  companyName: string;
  courses: ClinicContextCourse[];
};

export default function StudentClinicSchedulePage() {
  const { canRender, fallback } = useRoleGuard("STUDENT");
  const { contexts, isLoading, error, refresh } = useClinicContexts();
  const [selectedContextKey, setSelectedContextKey] = useState<string | null>(null);
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);

  const contextGroups = useMemo<ClinicContextGroup[]>(() => {
    const map = new Map<string, ClinicContextGroup>();

    contexts.forEach((context) => {
      const teacherId = context.teacherId ?? "";
      const branchId = context.branchId ?? "";
      const courseId = context.courseId ?? "";
      if (!teacherId || !branchId || !courseId) {
        return;
      }

      const key = `${teacherId}:${branchId}`;
      const existing = map.get(key);
      const course: ClinicContextCourse = {
        courseId,
        courseName: context.courseName ?? "반 이름 없음",
        recordId: context.recordId,
        defaultClinicSlotId: context.defaultClinicSlotId
      };

      if (existing) {
        const alreadyAdded = existing.courses.some((item) => item.courseId === courseId);
        if (!alreadyAdded) {
          existing.courses.push(course);
        }
      } else {
        map.set(key, {
          key,
          teacherId,
          branchId,
          teacherName: context.teacherName ?? "선생님",
          branchName: context.branchName ?? "지점",
          companyName: context.companyName ?? "학원",
          courses: [course]
        });
      }
    });

    return Array.from(map.values());
  }, [contexts]);

  useEffect(() => {
    if (contextGroups.length === 0) {
      if (selectedContextKey !== null) {
        setSelectedContextKey(null);
      }
      if (selectedCourseId !== null) {
        setSelectedCourseId(null);
      }
      return;
    }

    const nextContextKey =
      selectedContextKey && contextGroups.some((group) => group.key === selectedContextKey)
        ? selectedContextKey
        : contextGroups[0].key;
    const activeGroup = contextGroups.find((group) => group.key === nextContextKey);
    const nextCourseId =
      activeGroup && activeGroup.courses.some((course) => course.courseId === selectedCourseId)
        ? selectedCourseId
        : activeGroup?.courses[0]?.courseId ?? null;

    if (nextContextKey !== selectedContextKey) {
      setSelectedContextKey(nextContextKey);
    }
    if (nextCourseId !== selectedCourseId) {
      setSelectedCourseId(nextCourseId);
    }
  }, [contextGroups, selectedContextKey, selectedCourseId]);

  const handleSelectContext = useCallback((key: string) => {
    setSelectedContextKey(key);
  }, []);

  const handleRefresh = useCallback(() => {
    void refresh();
  }, [refresh]);

  if (!canRender) {
    return fallback;
  }
  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="클리닉 시간표" description="수업별 기본 슬롯을 선택하기 전에 선생님/지점 맥락을 먼저 고릅니다.">
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">선생님/지점 선택</p>
            <p className="text-xs text-slate-500">
              수강 중인 선생님과 지점을 선택하면 해당 반의 기본 슬롯을 설정할 수 있습니다.
            </p>
          </div>

          {error && (
            <div className="space-y-3">
              <InlineError message={error} />
              <Button variant="secondary" onClick={handleRefresh}>
                다시 불러오기
              </Button>
            </div>
          )}

          {isLoading && (
            <div className="grid gap-4 md:grid-cols-2">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-28 w-full" />
            </div>
          )}

          {!isLoading && contextGroups.length === 0 && (
            <EmptyState message="표시할 클리닉 정보가 없습니다." description="수강 중인 반이 없거나 배정 정보가 비어 있습니다." />
          )}

          {!isLoading && contextGroups.length > 0 && (
            <div className="grid gap-4 md:grid-cols-2">
              {contextGroups.map((group) => {
                const isSelected = group.key === selectedContextKey;
                return (
                  <button
                    key={group.key}
                    type="button"
                    onClick={() => handleSelectContext(group.key)}
                    className={clsx(
                      "rounded-2xl border p-4 text-left transition focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200",
                      isSelected
                        ? "border-blue-200 bg-blue-50/70 shadow-sm"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">
                          {group.teacherName} · {group.branchName}
                        </p>
                        <p className="text-xs text-slate-500">{group.companyName}</p>
                      </div>
                      {isSelected && <Badge>선택됨</Badge>}
                    </div>
                    <p className="mt-3 text-xs text-slate-500">반 {group.courses.length}개</p>

                    {isSelected && group.courses.length > 1 && (
                      <div className="mt-3">
                        <Select
                          label="반 선택"
                          value={selectedCourseId ?? ""}
                          onChange={(event) => setSelectedCourseId(event.target.value)}
                        >
                          {group.courses.map((course) => (
                            <option key={course.courseId} value={course.courseId}>
                              {course.courseName}
                            </option>
                          ))}
                        </Select>
                      </div>
                    )}

                    {isSelected && group.courses.length === 1 && (
                      <p className="mt-3 text-sm font-semibold text-slate-700">
                        반: {group.courses[0]?.courseName}
                      </p>
                    )}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </Card>

      <Card
        title="기본 슬롯 시간표"
        description="선택한 반 기준으로 기본 슬롯을 설정하는 시간표가 이어집니다."
      >
        <EmptyState message="기본 슬롯 시간표 준비 중입니다." description="선생님/지점 선택 후 시간표가 표시됩니다." />
      </Card>
    </div>
  );
}
