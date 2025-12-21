"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { ErrorState } from "@/components/ui/error-state";
import { ProgressCardList } from "@/components/dashboard/progress/progress-card-list";
import {
  ProgressFilterBar,
  type ProgressSelectOption
} from "@/components/dashboard/progress/progress-filter-bar";
import { fetchAssistantCourses, fetchTeacherCourses } from "@/lib/dashboard-api";
import { fetchCourseProgresses } from "@/lib/progress-api";
import type { CourseWithTeacherResponse, CourseResponse } from "@/types/dashboard";
import type { CourseProgressResponse, ProgressCursor } from "@/types/progress";

type ProgressRole = "TEACHER" | "ASSISTANT";

type CourseOption = {
  value: string;
  label: string;
};

type CourseProgressSectionProps = {
  role: ProgressRole;
};

export function CourseProgressSection({ role }: CourseProgressSectionProps) {
  const { showToast } = useToast();
  const [courseOptions, setCourseOptions] = useState<CourseOption[]>([]);
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [items, setItems] = useState<CourseProgressResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<ProgressCursor | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCourses = useCallback(async () => {
    try {
      setError(null);
      const response =
        role === "TEACHER"
          ? await fetchTeacherCourses({ status: "ACTIVE", page: 0, size: 50 })
          : await fetchAssistantCourses({ status: "ACTIVE", page: 0, size: 50 });
      const options = toCourseOptions(response.items);
      setCourseOptions(options);
      if (options.length > 0) {
        setSelectedCourseId((prev) => prev || options[0].value);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "반 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    }
  }, [role, showToast]);

  const loadProgresses = useCallback(
    async ({ cursor, append }: { cursor?: ProgressCursor | null; append: boolean }) => {
      if (!selectedCourseId) {
        return;
      }
      try {
        if (append) {
          setLoadingMore(true);
        } else {
          setLoading(true);
        }
        const response = await fetchCourseProgresses({ courseId: selectedCourseId, cursor });
        setItems((prev) => (append ? [...prev, ...response.items] : response.items));
        setNextCursor(response.nextCursor ?? null);
      } catch (err) {
        const message = err instanceof Error ? err.message : "공통 진도를 불러오지 못했습니다.";
        setError(message);
        showToast("error", message);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [selectedCourseId, showToast]
  );

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  useEffect(() => {
    if (!selectedCourseId) {
      setItems([]);
      setNextCursor(null);
      return;
    }
    void loadProgresses({ append: false });
  }, [loadProgresses, selectedCourseId]);

  const progressOptions = useMemo<ProgressSelectOption[]>(
    () => courseOptions.map((option) => ({ value: option.value, label: option.label })),
    [courseOptions]
  );

  if (error && courseOptions.length === 0) {
    return (
      <ErrorState
        title="반 목록을 불러오지 못했습니다"
        description={error}
        onRetry={() => void loadCourses()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <ProgressFilterBar
        label="반 선택"
        placeholder="반을 선택하세요"
        value={selectedCourseId}
        options={progressOptions}
        onChange={setSelectedCourseId}
        disabled={courseOptions.length === 0}
      />

      {loading && items.length === 0 ? (
        <ErrorState title="진도 데이터를 불러오는 중" description="잠시만 기다려 주세요." />
      ) : (
        <ProgressCardList
          items={items}
          emptyMessage="선택한 반의 기록이 없습니다."
          hasMore={Boolean(nextCursor)}
          loadingMore={loadingMore}
          onLoadMore={() => void loadProgresses({ cursor: nextCursor, append: true })}
        />
      )}
    </div>
  );
}

function toCourseOptions(courses: (CourseResponse | CourseWithTeacherResponse)[]): CourseOption[] {
  return courses
    .map((course) => ({
      value: course.courseId ?? "",
      label: course.name ? `${course.name}` : "이름 없는 반"
    }))
    .filter((option) => option.value.length > 0);
}
