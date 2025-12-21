"use client";

import { useEffect, useState } from "react";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { useToast } from "@/components/ui/toast";
import { fetchCourseProgresses, fetchPersonalProgresses, updateCourseProgress, updatePersonalProgress } from "@/lib/progress-api";
import type { ProgressCursor } from "@/types/progress";

type ProgressEditTarget = {
  type: "course" | "personal";
  id: string;
  courseId: string;
  recordId?: string;
  initialTitle?: string | null;
  initialContent?: string | null;
} | null;

type ProgressEditModalProps = {
  open: boolean;
  target: ProgressEditTarget;
  onClose: () => void;
  onSaved: () => void;
};

type ProgressSliceResult = {
  items: { id?: string; title?: string; content?: string }[];
  nextCursor?: ProgressCursor;
};

const DETAIL_PAGE_LIMIT = 50;
const MAX_PAGES = 5;

export function ProgressEditModal({ open, target, onClose, onSaved }: ProgressEditModalProps) {
  const { showToast } = useToast();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !target) {
      return;
    }
    if (target.initialTitle || target.initialContent) {
      setTitle(target.initialTitle ?? "");
      setContent(target.initialContent ?? "");
      setError(null);
      setLoading(false);
      return;
    }
    const loadDetail = async () => {
      setLoading(true);
      setError(null);
      try {
        if (target.type === "personal" && !target.recordId) {
          setError("개인 진도 정보를 찾을 수 없습니다.");
          setLoading(false);
          return;
        }
        let cursor: ProgressCursor | null = null;
        for (let page = 0; page < MAX_PAGES; page += 1) {
          const response: ProgressSliceResult =
            target.type === "course"
              ? await fetchCourseProgresses({ courseId: target.courseId, cursor, limit: DETAIL_PAGE_LIMIT })
              : await fetchPersonalProgresses({ recordId: target.recordId ?? "", cursor, limit: DETAIL_PAGE_LIMIT });
          const found = response.items.find((item) => item.id === target.id);
          if (found) {
            setTitle(found.title ?? "");
            setContent(found.content ?? "");
            setLoading(false);
            return;
          }
          if (!response.nextCursor) {
            break;
          }
          cursor = response.nextCursor;
        }
        setError("진도 상세 정보를 찾지 못했습니다.");
      } catch (err) {
        const message = err instanceof Error ? err.message : "진도 정보를 불러오지 못했습니다.";
        setError(message);
      } finally {
        setLoading(false);
      }
    };
    void loadDetail();
  }, [open, target]);

  const handleSave = async () => {
    if (!target) {
      return;
    }
    if (!title.trim() || !content.trim()) {
      setError("제목과 내용을 모두 입력해주세요.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      if (target.type === "course") {
        await updateCourseProgress(target.id, { title: title.trim(), content: content.trim() });
      } else {
        await updatePersonalProgress(target.id, { title: title.trim(), content: content.trim() });
      }
      showToast("success", "진도를 수정했습니다.");
      onSaved();
      onClose();
    } catch (err) {
      const message = err instanceof Error ? err.message : "진도를 수정하지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="진도 수정" size="lg">
      <div className="space-y-4">
        {error ? <InlineError message={error} /> : null}
        <div className="flex flex-col gap-2">
          <label className="text-sm font-medium text-slate-700">
            제목 <span className="text-rose-500">*</span>
          </label>
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            className="h-12 rounded-2xl border border-slate-200 px-4 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
          />
        </div>
        <div className="flex flex-col gap-2">
          <label className="text-sm font-medium text-slate-700">
            내용 <span className="text-rose-500">*</span>
          </label>
          <textarea
            rows={4}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            className="rounded-2xl border border-slate-200 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
          />
        </div>
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            닫기
          </Button>
          <Button onClick={handleSave} disabled={loading}>
            {loading ? "저장 중..." : "저장"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
