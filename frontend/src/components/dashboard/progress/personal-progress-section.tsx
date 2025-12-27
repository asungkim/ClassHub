"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { ErrorState } from "@/components/ui/error-state";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";
import { DatePicker } from "@/components/ui/date-picker";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useSession } from "@/components/session/session-provider";
import { ProgressCardList } from "@/components/dashboard/progress/progress-card-list";
import {
  fetchPersonalProgresses,
  createPersonalProgress,
  updatePersonalProgress,
  deletePersonalProgress
} from "@/lib/progress-api";
import { fetchTeacherStudentDetail, fetchTeacherStudents } from "@/lib/dashboard-api";
import { formatStudentGrade } from "@/utils/student";
import type { StudentSummaryResponse, TeacherStudentCourseResponse } from "@/types/dashboard";
import type { PersonalProgressResponse, ProgressCursor } from "@/types/progress";

type PersonalProgressSectionProps = {
  role: "TEACHER" | "ASSISTANT";
};

const SEARCH_DEBOUNCE_MS = 300;

export function PersonalProgressSection({ role }: PersonalProgressSectionProps) {
  const { showToast } = useToast();
  const { member } = useSession();
  const [searchValue, setSearchValue] = useState("");
  const [searchResults, setSearchResults] = useState<StudentSummaryResponse[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState<StudentSummaryResponse | null>(null);
  const [studentCourses, setStudentCourses] = useState<TeacherStudentCourseResponse[]>([]);
  const [selectedRecordId, setSelectedRecordId] = useState("");
  const [items, setItems] = useState<PersonalProgressResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<ProgressCursor | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const searchRequestId = useRef(0);

  // 작성 모달 상태
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [formData, setFormData] = useState({ date: "", title: "", content: "" });
  const [submitting, setSubmitting] = useState(false);

  // 수정 모달 상태
  const [editingItem, setEditingItem] = useState<PersonalProgressResponse | null>(null);
  const [editFormData, setEditFormData] = useState({ date: "", title: "", content: "" });
  const [updating, setUpdating] = useState(false);

  // 삭제 확인 상태
  const [deletingItemId, setDeletingItemId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadStudents = useCallback(async (keyword?: string, requestId?: number) => {
    try {
      setError(null);
      setSearchLoading(true);
      const trimmedKeyword = keyword?.trim();
      const response = await fetchTeacherStudents({
        page: 0,
        size: 100,
        keyword: trimmedKeyword && trimmedKeyword.length > 0 ? trimmedKeyword : undefined
      });
      if (requestId && requestId !== searchRequestId.current) {
        return;
      }
      setSearchResults(dedupeStudents(response.items));
    } catch (err) {
      const message = err instanceof Error ? err.message : "학생 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setSearchLoading(false);
    }
  }, [showToast]);

  const loadProgresses = useCallback(
    async ({ cursor, append }: { cursor?: ProgressCursor | null; append: boolean }) => {
      if (!selectedRecordId) {
        return;
      }
      try {
        if (append) {
          setLoadingMore(true);
        } else {
          setLoading(true);
        }
        const response = await fetchPersonalProgresses({ recordId: selectedRecordId, cursor });
        setItems((prev) => (append ? [...prev, ...response.items] : response.items));
        setNextCursor(response.nextCursor ?? null);
      } catch (err) {
        const message = err instanceof Error ? err.message : "개인 진도를 불러오지 못했습니다.";
        setError(message);
        showToast("error", message);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [selectedRecordId, showToast]
  );

  useEffect(() => {
    const trimmed = searchValue.trim();
    if (!trimmed) {
      setSearchResults([]);
      return;
    }
    if (selectedStudent && trimmed === selectedStudent.name) {
      return;
    }
    const timer = setTimeout(() => {
      const requestId = ++searchRequestId.current;
      void loadStudents(trimmed, requestId);
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [loadStudents, searchValue, selectedStudent]);

  const loadStudentDetail = useCallback(
    async (studentId: string) => {
      try {
        const detail = await fetchTeacherStudentDetail(studentId);
        setStudentCourses(detail.courses ?? []);
        const recordIds = (detail.courses ?? [])
          .map((course) => course.recordId ?? "")
          .filter((recordId) => recordId.length > 0);
        if (recordIds.length === 1) {
          setSelectedRecordId(recordIds[0]);
        } else {
          setSelectedRecordId("");
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : "학생 상세 정보를 불러오지 못했습니다.";
        setError(message);
        showToast("error", message);
      }
    },
    [showToast]
  );

  useEffect(() => {
    if (!selectedStudent?.memberId) {
      setStudentCourses([]);
      setSelectedRecordId("");
      return;
    }
    void loadStudentDetail(selectedStudent.memberId);
  }, [loadStudentDetail, selectedStudent]);

  useEffect(() => {
    if (!selectedRecordId) {
      setItems([]);
      setNextCursor(null);
      return;
    }
    void loadProgresses({ append: false });
  }, [loadProgresses, selectedRecordId]);

  const handleCreateSubmit = useCallback(async () => {
    if (!selectedRecordId) {
      showToast("error", "학생과 반을 먼저 선택해주세요.");
      return;
    }
    if (!formData.date || !formData.title) {
      showToast("error", "날짜와 제목은 필수입니다.");
      return;
    }

    try {
      setSubmitting(true);
      await createPersonalProgress(selectedRecordId, {
        date: formData.date,
        title: formData.title,
        content: formData.content
      });
      showToast("success", "개인 진도가 저장되었습니다.");
      setCreateModalOpen(false);
      setFormData({ date: "", title: "", content: "" });
      await loadProgresses({ append: false });
    } catch (err) {
      const message = err instanceof Error ? err.message : "진도 저장에 실패했습니다.";
      showToast("error", message);
    } finally {
      setSubmitting(false);
    }
  }, [selectedRecordId, formData, showToast, loadProgresses]);

  const handleEdit = useCallback((item: PersonalProgressResponse) => {
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
      await updatePersonalProgress(editingItem.id, {
        date: editFormData.date,
        title: editFormData.title,
        content: editFormData.content
      });
      showToast("success", "개인 진도가 수정되었습니다.");
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

  const handleDelete = useCallback((item: PersonalProgressResponse) => {
    setDeletingItemId(item.id ?? null);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!deletingItemId) {
      return;
    }

    try {
      setDeleting(true);
      await deletePersonalProgress(deletingItemId);
      showToast("success", "개인 진도가 삭제되었습니다.");
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
    (item: PersonalProgressResponse) => {
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

  const courseOptions = useMemo(
    () =>
      studentCourses
        .map((course) => ({
          value: course.recordId ?? "",
          label: course.name ?? "반"
        }))
        .filter((option) => option.value.length > 0),
    [studentCourses]
  );

  const canWrite = member?.role === "TEACHER" || member?.role === "ASSISTANT";

  if (error && searchResults.length === 0 && !selectedStudent) {
    return (
      <ErrorState
        title="학생 목록을 불러오지 못했습니다"
        description={error}
        onRetry={() => void loadStudents(searchValue)}
      />
    );
  }

  const selectedCourse = courseOptions.find((option) => option.value === selectedRecordId);

  return (
    <>
      <div className="space-y-6">
        {/* 학생 & 반 선택 카드 */}
        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-4">
            <h3 className="text-base font-semibold text-slate-900">개인 진도 관리</h3>
            <p className="text-sm text-slate-500">학생을 검색하고 개인 진도를 작성하세요.</p>
          </div>

          {/* 학생 검색 */}
          <div className="relative mb-4">
            <TextField
              label="학생 검색"
              placeholder="학생 이름을 입력하세요"
              value={searchValue}
              onChange={(event) => {
                const value = event.target.value;
                setSearchValue(value);
                if (selectedStudent && value !== selectedStudent.name) {
                  setSelectedStudent(null);
                  setSelectedRecordId("");
                  setItems([]);
                  setNextCursor(null);
                }
              }}
            />
            {searchLoading ? <p className="mt-2 text-xs text-slate-400">검색 중...</p> : null}
            {searchValue.trim().length > 0 && searchResults.length > 0 ? (
              <div className="absolute left-0 right-0 top-full z-10 mt-2 max-h-60 overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">
                {searchResults.map((student) => (
                  <button
                    key={student.memberId ?? student.name}
                    type="button"
                    onClick={() => {
                      searchRequestId.current += 1;
                      setSelectedStudent(student);
                      setSearchValue(student.name ?? "");
                      setSearchResults([]);
                    }}
                    className="w-full rounded-xl px-4 py-3 text-left text-sm hover:bg-slate-50"
                  >
                    <p className="font-semibold text-slate-900">{student.name ?? "학생"}</p>
                    <p className="text-xs text-slate-500">{formatStudentSummary(student)}</p>
                  </button>
                ))}
              </div>
            ) : null}
          </div>

          {/* 반 선택 & 진도 작성 버튼 */}
          {selectedStudent && courseOptions.length > 1 && (
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
              <div className="flex-1">
                <label className="mb-2 block text-sm font-medium text-slate-700">반 선택</label>
                <select
                  value={selectedRecordId}
                  onChange={(e) => setSelectedRecordId(e.target.value)}
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
              {canWrite && selectedRecordId && (
                <Button variant="primary" onClick={() => setCreateModalOpen(true)} className="sm:mb-0">
                  + 진도 작성
                </Button>
              )}
            </div>
          )}

          {/* 반이 하나뿐일 때 */}
          {selectedStudent && courseOptions.length === 1 && canWrite && selectedRecordId && (
            <Button variant="primary" onClick={() => setCreateModalOpen(true)} className="w-full">
              + 진도 작성
            </Button>
          )}

          {/* 선택된 학생/반 표시 */}
          {selectedStudent && selectedRecordId && selectedCourse && (
            <div className="mt-4 rounded-2xl bg-blue-50 px-4 py-3">
              <p className="text-sm font-semibold text-blue-900">
                👤 학생: <span className="text-blue-700">{selectedStudent.name}</span>
                {courseOptions.length > 1 && (
                  <>
                    {" · "}📚 반: <span className="text-blue-700">{selectedCourse.label}</span>
                  </>
                )}
              </p>
            </div>
          )}
        </div>

      {loading && items.length === 0 ? (
        <ErrorState title="진도 데이터를 불러오는 중" description="잠시만 기다려 주세요." />
      ) : (
        <ProgressCardList
          items={items}
          emptyMessage={
            selectedRecordId
              ? "선택한 학생의 기록이 없습니다."
              : selectedStudent
                ? "반을 선택해 주세요."
                : "학생을 검색해 선택해 주세요."
          }
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
        title="개인 진도 작성"
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
        title="개인 진도 수정"
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

function formatStudentSummary(student: StudentSummaryResponse) {
  const schoolName = student.schoolName ?? "학교 정보 없음";
  const gradeLabel = formatStudentGrade(student.grade);
  if (!gradeLabel) {
    return schoolName;
  }
  return `${schoolName}(${gradeLabel})`;
}

function dedupeStudents(students: StudentSummaryResponse[]) {
  const seen = new Set<string>();
  return students.filter((student) => {
    const key = student.memberId ?? student.email ?? student.name ?? "";
    if (!key) {
      return true;
    }
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}
