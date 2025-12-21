"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import type { CourseProgressResponse, PersonalProgressResponse } from "@/types/progress";

type ProgressItem = CourseProgressResponse | PersonalProgressResponse;

type ProgressCardListProps = {
  items: ProgressItem[];
  emptyMessage: string;
  onLoadMore?: () => void;
  hasMore?: boolean;
  loadingMore?: boolean;
};

export function ProgressCardList({ items, emptyMessage, onLoadMore, hasMore, loadingMore }: ProgressCardListProps) {
  if (items.length === 0) {
    return (
      <Card title="진도 기록" description={emptyMessage}>
        <p className="text-sm text-slate-500">필터를 선택하면 기록이 표시됩니다.</p>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {items.map((item) => (
        <Card key={item.id}>
          <div className="space-y-3">
            <div className="flex items-start justify-between text-xs text-slate-400">
              <p className="font-semibold">{formatDate(item.date)}</p>
              <p>작성자: {formatRole(item.writerRole)}</p>
            </div>
            <div className="space-y-2">
              <p className="text-base font-semibold text-slate-900">{item.title ?? "제목 없음"}</p>
              <p className="text-sm text-slate-600 line-clamp-2 whitespace-pre-line">
                {item.content || "내용이 없습니다."}
              </p>
            </div>
          </div>
        </Card>
      ))}
      {hasMore && onLoadMore ? (
        <div className="flex justify-center">
          <Button variant="secondary" onClick={onLoadMore} disabled={loadingMore}>
            {loadingMore ? "불러오는 중..." : "더 보기"}
          </Button>
        </div>
      ) : null}
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return "날짜 정보 없음";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric"
  });
}

function formatRole(role?: string | null) {
  switch (role) {
    case "TEACHER":
      return "선생님";
    case "ASSISTANT":
      return "조교";
    case "STUDENT":
      return "학생";
    case "ADMIN":
      return "관리자";
    case "SUPER_ADMIN":
      return "슈퍼관리자";
    default:
      return "알 수 없음";
  }
}
