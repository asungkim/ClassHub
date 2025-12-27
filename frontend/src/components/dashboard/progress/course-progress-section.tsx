"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { ErrorState } from "@/components/ui/error-state";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";
import { DatePicker } from "@/components/ui/date-picker";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ProgressCardList } from "@/components/dashboard/progress/progress-card-list";
import { useSession } from "@/components/session/session-provider";
import { fetchAssistantCourses, fetchTeacherCourses } from "@/lib/dashboard-api";
import { fetchCourseProgresses, createCourseProgress, updateCourseProgress, deleteCourseProgress } from "@/lib/progress-api";
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
  const { member } = useSession();
  const [courseOptions, setCourseOptions] = useState<CourseOption[]>([]);
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [items, setItems] = useState<CourseProgressResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<ProgressCursor | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 작성 모달 상태
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [formData, setFormData] = useState({ date: "", title: "", content: "" });
  const [submitting, setSubmitting] = useState(false);

  // 수정 모달 상태
  const [editingItem, setEditingItem] = useState<CourseProgressResponse | null>(null);
  const [editFormData, setEditFormData] = useState({ date: "", title: "", content: "" });
  const [updating, setUpdating] = useState(false);

  // 삭제 확인 상태
  const [deletingItemId, setDeletingItemId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

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

  const handleCreateSubmit = useCallback(async () => {
    if (!selectedCourseId) {
      showToast("error", "반을 먼저 선택해주세요.");
      return;
    }
    if (!formData.date || !formData.title) {
      showToast("error", "날짜와 제목은 필수입니다.");
      return;
    }

    try {
      setSubmitting(true);
      await createCourseProgress(selectedCourseId, {
        date: formData.date,
        title: formData.title,
        content: formData.content
      });
      showToast("success", "공통 진도가 저장되었습니다.");
      setCreateModalOpen(false);
      setFormData({ date: "", title: "", content: "" });
      await loadProgresses({ append: false });
    } catch (err) {
      const message = err instanceof Error ? err.message : "진도 저장에 실패했습니다.";
      showToast("error", message);
    } finally {
      setSubmitting(false);
    }
  }, [selectedCourseId, formData, showToast, loadProgresses]);

  const handleEdit = useCallback((item: CourseProgressResponse) => {
    setEditingItem(item);
    setEditFormData({
      date: item.date ?? "",
      title: item.title ?? "",
      content: item.content ?? ""
    });
  }, []);

  const handleUpdateSubmit = useCallback(async () => {
    if (!editingItem?.id) {
      return;
    }
    if (!editFormData.date || !editFormData.title) {
      showToast("error", "날짜와 제목은 필수입니다.");
      return;
    }

    try {
      setUpdating(true);
      await updateCourseProgress(editingItem.id, {
        date: editFormData.date,
        title: editFormData.title,
        content: editFormData.content
      });
      showToast("success", "공통 진도가 수정되었습니다.");
      setEditingItem(null);
      setEditFormData({ date: "", title: "", content: "" });
      await loadProgresses({ append: false });
    } catch (err) {
      const message = err instanceof Error ? err.message : "진도 수정에 실패했습니다.";
      showToast("error", message);
    } finally {
      setUpdating(false);
    }
  }, [editingItem, editFormData, showToast, loadProgresses]);

  const handleDelete = useCallback((item: CourseProgressResponse) => {
    setDeletingItemId(item.id ?? null);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!deletingItemId) {
      return;
    }

    try {
      setDeleting(true);
      await deleteCourseProgress(deletingItemId);
      showToast("success", "공통 진도가 삭제되었습니다.");
      setDeletingItemId(null);
      await loadProgresses({ append: false });
    } catch (err) {
      const message = err instanceof Error ? err.message : "진도 삭제에 실패했습니다.";
      showToast("error", message);
    } finally {
      setDeleting(false);
    }
  }, [deletingItemId, showToast, loadProgresses]);

  const canEditDelete = useCallback(
    (item: CourseProgressResponse) => {
      if (!member?.memberId) {
        return false;
      }
      if (member.role === "TEACHER") {
        return true;
      }
      return item.writerId === member.memberId;
    },
    [member]
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

  const canWrite = member?.role === "TEACHER" || member?.role === "ASSISTANT";

  const selectedCourse = courseOptions.find((option) => option.value === selectedCourseId);

  return (
    <>
      <div className="space-y-6">
        {/* 반 선택 & 진도 작성 카드 */}
        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-4">
            <h3 className="text-base font-semibold text-slate-900">진도 관리</h3>
            <p className="text-sm text-slate-500">반을 선택하고 공통 진도를 작성하세요.</p>
          </div>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="flex-1">
              <label className="mb-2 block text-sm font-medium text-slate-700">반 선택</label>
              <select
                value={selectedCourseId}
                onChange={(e) => setSelectedCourseId(e.target.value)}
                disabled={courseOptions.length === 0}
                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200 disabled:opacity-50"
              >
                <option value="">반을 선택하세요</option>
                {courseOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            {canWrite && selectedCourseId && (
              <Button
                variant="primary"
                onClick={() => setCreateModalOpen(true)}
                className="sm:mb-0"
              >
                + 진도 작성
              </Button>
            )}
          </div>
          {selectedCourse && (
            <div className="mt-4 rounded-2xl bg-blue-50 px-4 py-3">
              <p className="text-sm font-semibold text-blue-900">
                📚 선택된 반: <span className="text-blue-700">{selectedCourse.label}</span>
              </p>
            </div>
          )}
        </div>

        {loading && items.length === 0 ? (
          <ErrorState title="진도 데이터를 불러오는 중" description="잠시만 기다려 주세요." />
        ) : (
          <ProgressCardList
            items={items}
            emptyMessage="선택한 반의 기록이 없습니다."
            hasMore={Boolean(nextCursor)}
            loadingMore={loadingMore}
            onLoadMore={() => void loadProgresses({ cursor: nextCursor, append: true })}
            onEdit={handleEdit}
            onDelete={handleDelete}
            canEditDelete={canEditDelete}
          />
        )}
      </div>

      {/* 작성 모달 */}
      <Modal
        open={createModalOpen}
        onClose={() => {
          setCreateModalOpen(false);
          setFormData({ date: "", title: "", content: "" });
        }}
        title="공통 진도 작성"
      >
        <div className="space-y-4">
          <DatePicker
            label="수업 날짜"
            required
            value={formData.date}
            onChange={(date) => setFormData((prev) => ({ ...prev, date }))}
          />
          <TextField
            label="제목"
            required
            placeholder="진도 제목을 입력하세요"
            value={formData.title}
            onChange={(e) => setFormData((prev) => ({ ...prev, title: e.target.value }))}
          />
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-slate-700">내용</label>
            <textarea
              rows={6}
              placeholder="진도 내용을 입력하세요"
              value={formData.content}
              onChange={(e) => setFormData((prev) => ({ ...prev, content: e.target.value }))}
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div className="flex justify-end gap-2 pt-4">
            <Button
              variant="secondary"
              onClick={() => {
                setCreateModalOpen(false);
                setFormData({ date: "", title: "", content: "" });
              }}
              disabled={submitting}
            >
              취소
            </Button>
            <Button variant="primary" onClick={handleCreateSubmit} disabled={submitting}>
              {submitting ? "저장 중..." : "저장"}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 수정 모달 */}
      <Modal
        open={Boolean(editingItem)}
        onClose={() => {
          setEditingItem(null);
          setEditFormData({ date: "", title: "", content: "" });
        }}
        title="공통 진도 수정"
      >
        <div className="space-y-4">
          <DatePicker
            label="수업 날짜"
            required
            value={editFormData.date}
            onChange={(date) => setEditFormData((prev) => ({ ...prev, date }))}
          />
          <TextField
            label="제목"
            required
            placeholder="진도 제목을 입력하세요"
            value={editFormData.title}
            onChange={(e) => setEditFormData((prev) => ({ ...prev, title: e.target.value }))}
          />
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-slate-700">내용</label>
            <textarea
              rows={6}
              placeholder="진도 내용을 입력하세요"
              value={editFormData.content}
              onChange={(e) => setEditFormData((prev) => ({ ...prev, content: e.target.value }))}
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div className="flex justify-end gap-2 pt-4">
            <Button
              variant="secondary"
              onClick={() => {
                setEditingItem(null);
                setEditFormData({ date: "", title: "", content: "" });
              }}
              disabled={updating}
            >
              취소
            </Button>
            <Button variant="primary" onClick={handleUpdateSubmit} disabled={updating}>
              {updating ? "수정 중..." : "수정"}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 삭제 확인 다이얼로그 */}
      <ConfirmDialog
        open={Boolean(deletingItemId)}
        title="진도 삭제"
        message="이 진도 기록을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다."
        confirmText="삭제"
        cancelText="취소"
        onConfirm={handleDeleteConfirm}
        onClose={() => setDeletingItemId(null)}
        isLoading={deleting}
      />
    </>
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
