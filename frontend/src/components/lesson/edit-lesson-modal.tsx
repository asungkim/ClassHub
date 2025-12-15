"use client";

import { useState } from "react";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";

export type EditLessonModalProps = {
  open: boolean;
  onClose: () => void;
  onSave: (data: { title?: string; content?: string }) => Promise<void>;
  type: "shared" | "personal";
  initialData: {
    title?: string;
    content?: string;
  };
  isLoading?: boolean;
};

export function EditLessonModal({
  open,
  onClose,
  onSave,
  type,
  initialData,
  isLoading = false
}: EditLessonModalProps) {
  const [title, setTitle] = useState(initialData.title ?? "");
  const [content, setContent] = useState(initialData.content ?? "");

  const handleSave = async () => {
    const data: { title?: string; content?: string } = {};

    if (type === "shared") {
      data.title = title;
      data.content = content;
    } else {
      data.content = content;
    }

    await onSave(data);
  };

  return (
    <Modal open={open} onClose={onClose} title={type === "shared" ? "공통 진도 수정" : "개인 진도 수정"} size="md">
      <div className="space-y-4">
        {type === "shared" && (
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">제목</label>
            <TextField
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="제목을 입력하세요"
              disabled={isLoading}
            />
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">내용</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="내용을 입력하세요"
            disabled={isLoading}
            rows={6}
            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-900 placeholder-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:opacity-50"
          />
        </div>

        <div className="flex gap-3 justify-end pt-4">
          <Button variant="ghost" onClick={onClose} disabled={isLoading}>
            취소
          </Button>
          <Button onClick={handleSave} disabled={isLoading}>
            {isLoading ? "저장 중..." : "저장"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
