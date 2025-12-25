"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";
import { DatePicker } from "@/components/ui/date-picker";
import { InlineError } from "@/components/ui/inline-error";
import { Checkbox } from "@/components/ui/checkbox";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useToast } from "@/components/ui/toast";
import { fetchTeacherCourses, fetchStudentCourseRecords } from "@/lib/dashboard-api";
import { composeCourseProgress, createCourseProgress, createPersonalProgress } from "@/lib/progress-api";
import { formatDateYmdKst } from "@/utils/date";
import type { CourseResponse, StudentCourseListItemResponse } from "@/types/dashboard";
import type { CourseProgressCreateRequest, PersonalProgressComposeRequest } from "@/types/progress";

type PersonalInput = {
  title: string;
  content: string;
};

type TeacherLessonComposeModalProps = {
  open: boolean;
  onClose: () => void;
};

const todayString = () => formatDateYmdKst(new Date());

export function TeacherLessonComposeModal({ open, onClose }: TeacherLessonComposeModalProps) {
  const { showToast } = useToast();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [courseDate, setCourseDate] = useState(todayString());
  const [courseTitle, setCourseTitle] = useState("");
  const [courseContent, setCourseContent] = useState("");
  const [students, setStudents] = useState<StudentCourseListItemResponse[]>([]);
  const [selectedStudentIds, setSelectedStudentIds] = useState<string[]>([]);
  const [personalInputs, setPersonalInputs] = useState<Record<string, PersonalInput>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const studentMap = useMemo(() => {
    return students.reduce<Record<string, StudentCourseListItemResponse>>((acc, student) => {
      if (student.recordId) {
        acc[student.recordId] = student;
      }
      return acc;
    }, {});
  }, [students]);

  const isDirty =
    Boolean(courseTitle.trim()) ||
    Boolean(courseContent.trim()) ||
    selectedStudentIds.length > 0;

  const resetState = useCallback(() => {
    setSelectedCourseId("");
    setCourseDate(todayString());
    setCourseTitle("");
    setCourseContent("");
    setStudents([]);
    setSelectedStudentIds([]);
    setPersonalInputs({});
    setSubmitError(null);
    setLoading(false);
  }, []);

  const handleRequestClose = useCallback(() => {
    if (isDirty) {
      setConfirmOpen(true);
      return;
    }
    resetState();
    onClose();
  }, [isDirty, onClose, resetState]);

  const loadCourses = useCallback(async () => {
    try {
      const response = await fetchTeacherCourses({ status: "ACTIVE", page: 0, size: 50 });
      setCourses(response.items);
      if (response.items.length > 0) {
        const firstCourseId = response.items[0]?.courseId ?? "";
        setSelectedCourseId((prev) => prev || firstCourseId);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : "반 목록을 불러오지 못했습니다.";
      showToast("error", message);
    }
  }, [showToast]);

  const loadStudents = useCallback(
    async (courseId: string) => {
      if (!courseId) {
        setStudents([]);
        setSelectedStudentIds([]);
        setPersonalInputs({});
        return;
      }
      try {
        const response = await fetchStudentCourseRecords({ courseId, status: "ACTIVE", page: 0, size: 50 });
        setStudents(response.items);
        setSelectedStudentIds([]);
        setPersonalInputs({});
      } catch (error) {
        const message = error instanceof Error ? error.message : "학생 목록을 불러오지 못했습니다.";
        showToast("error", message);
      }
    },
    [showToast]
  );

  useEffect(() => {
    if (!open) {
      return;
    }
    void loadCourses();
  }, [loadCourses, open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    void loadStudents(selectedCourseId);
  }, [loadStudents, open, selectedCourseId]);

  useEffect(() => {
    if (!open) {
      resetState();
    }
  }, [open, resetState]);

  const toggleStudent = (recordId: string) => {
    setSelectedStudentIds((prev) => {
      if (prev.includes(recordId)) {
        const next = prev.filter((id) => id !== recordId);
        setPersonalInputs((inputs) => {
          const copy = { ...inputs };
          delete copy[recordId];
          return copy;
        });
        return next;
      }
      setPersonalInputs((inputs) => ({
        ...inputs,
        [recordId]: { title: "", content: "" }
      }));
      return [...prev, recordId];
    });
  };

  const updatePersonalInput = (recordId: string, field: keyof PersonalInput, value: string) => {
    setPersonalInputs((prev) => ({
      ...prev,
      [recordId]: {
        ...prev[recordId],
        [field]: value
      }
    }));
  };

  const handleSubmit = async () => {
    if (!selectedCourseId) {
      setSubmitError("반을 먼저 선택해주세요.");
      return;
    }

    const hasCourseProgress = Boolean(courseTitle.trim() && courseContent.trim());
    const hasPersonalProgress = selectedStudentIds.length > 0;

    // 최소 하나는 입력되어야 함
    if (!hasCourseProgress && !hasPersonalProgress) {
      setSubmitError("공통 진도 또는 개인 진도 중 최소 하나를 입력해주세요.");
      return;
    }

    // 개인 진도 검증
    if (hasPersonalProgress) {
      const personalRequests: PersonalProgressComposeRequest[] = selectedStudentIds.map((recordId) => ({
        studentCourseRecordId: recordId,
        date: courseDate,
        title: personalInputs[recordId]?.title?.trim() ?? "",
        content: personalInputs[recordId]?.content?.trim() ?? ""
      }));
      const invalidPersonal = personalRequests.some((request) => !request.title || !request.content);
      if (invalidPersonal) {
        setSubmitError("선택한 학생의 제목과 내용을 모두 입력해주세요.");
        return;
      }
    }

    setSubmitError(null);
    setLoading(true);
    try {
      // Case 1: 공통 진도만 작성
      if (hasCourseProgress && !hasPersonalProgress) {
        const courseRequest: CourseProgressCreateRequest = {
          date: courseDate,
          title: courseTitle.trim(),
          content: courseContent.trim()
        };
        await createCourseProgress(selectedCourseId, courseRequest);
        showToast("success", "공통 진도를 저장했습니다.");
      }
      // Case 2: 공통 진도 + 개인 진도
      else if (hasCourseProgress && hasPersonalProgress) {
        const courseRequest: CourseProgressCreateRequest = {
          date: courseDate,
          title: courseTitle.trim(),
          content: courseContent.trim()
        };
        const personalRequests: PersonalProgressComposeRequest[] = selectedStudentIds.map((recordId) => ({
          studentCourseRecordId: recordId,
          date: courseDate,
          title: personalInputs[recordId]?.title?.trim() ?? "",
          content: personalInputs[recordId]?.content?.trim() ?? ""
        }));
        await composeCourseProgress(selectedCourseId, {
          courseProgress: courseRequest,
          personalProgressList: personalRequests
        });
        showToast("success", "공통 진도와 개인 진도를 저장했습니다.");
      }
      // Case 3: 개인 진도만 작성
      else if (!hasCourseProgress && hasPersonalProgress) {
        await Promise.all(
          selectedStudentIds.map((recordId) =>
            createPersonalProgress(recordId, {
              date: courseDate,
              title: personalInputs[recordId]?.title?.trim() ?? "",
              content: personalInputs[recordId]?.content?.trim() ?? ""
            })
          )
        );
        showToast("success", "개인 진도를 저장했습니다.");
      }

      resetState();
      onClose();
    } catch (error) {
      const message = error instanceof Error ? error.message : "진도를 저장하지 못했습니다.";
      setSubmitError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Modal
        open={open}
        onClose={handleRequestClose}
        title="통합 수업 작성"
        size="lg"
        mobileLayout="bottom-sheet"
      >
        <div className="space-y-6">
          {submitError ? <InlineError message={submitError} /> : null}

          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-slate-900">반 선택</h3>
                <p className="text-sm text-slate-500">공통 진도를 작성할 반을 선택하세요.</p>
              </div>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              {courses.map((course) => {
                const isSelected = course.courseId === selectedCourseId;
                return (
                  <button
                    key={course.courseId}
                    type="button"
                    onClick={() => setSelectedCourseId(course.courseId ?? "")}
                    className={clsx(
                      "rounded-2xl border px-4 py-3 text-left transition",
                      isSelected
                        ? "border-blue-400 bg-blue-50 shadow-sm"
                        : "border-slate-200 hover:border-blue-200 hover:bg-slate-50"
                    )}
                  >
                    <p className="text-sm font-semibold text-slate-900">{course.name ?? "이름 없는 반"}</p>
                    <p className="text-xs text-slate-500">
                      {course.companyName ?? "학원"} · {course.branchName ?? "지점"}
                    </p>
                  </button>
                );
              })}
              {courses.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-6 text-center text-sm text-slate-400">
                  등록된 반이 없습니다.
                </div>
              ) : null}
            </div>
          </section>

          <section className="space-y-4">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">공통 진도 (선택)</h3>
              <p className="text-sm text-slate-500">반 공통 진도 내용을 입력하세요. 개인 진도만 작성할 수도 있습니다.</p>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <DatePicker
                label="수업 날짜"
                value={courseDate}
                onChange={setCourseDate}
              />
              <TextField
                label="제목"
                placeholder="예: 3월 2주차 수업"
                value={courseTitle}
                onChange={(event) => setCourseTitle(event.target.value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-slate-700">
                내용
              </label>
              <textarea
                rows={4}
                placeholder="수업 내용을 입력하세요."
                value={courseContent}
                onChange={(event) => setCourseContent(event.target.value)}
                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </div>
          </section>

          <section className="space-y-4">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">학생별 개인 진도</h3>
              <p className="text-sm text-slate-500">선택한 학생에 대해서만 개인 진도를 입력합니다.</p>
            </div>
            <div className="grid gap-2 md:grid-cols-2">
              {students.map((student) => {
                const recordId = student.recordId ?? "";
                const isChecked = Boolean(recordId && selectedStudentIds.includes(recordId));
                return (
                  <div key={recordId} className="rounded-2xl border border-slate-200 px-4 py-3">
                    <Checkbox
                      checked={isChecked}
                      onChange={() => recordId && toggleStudent(recordId)}
                      label={
                        <span className="flex flex-col text-left">
                          <span className="text-sm font-semibold text-slate-900">
                            {student.studentName ?? "학생"}
                          </span>
                          <span className="text-xs text-slate-500">
                            {student.courseName ?? "반 정보 없음"}
                          </span>
                        </span>
                      }
                    />
                  </div>
                );
              })}
              {students.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-6 text-center text-sm text-slate-400">
                  선택한 반에 학생이 없습니다.
                </div>
              ) : null}
            </div>

            {selectedStudentIds.length > 0 ? (
              <div className="space-y-4">
                {selectedStudentIds.map((recordId) => {
                  const student = studentMap[recordId];
                  const input = personalInputs[recordId] ?? { title: "", content: "" };
                  return (
                    <div key={recordId} className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
                      <div className="mb-4">
                        <p className="text-sm font-semibold text-slate-900">
                          {student?.studentName ?? "학생"} · {student?.courseName ?? "반"}
                        </p>
                        <p className="text-xs text-slate-500">개인 진도 입력</p>
                      </div>
                      <div className="grid gap-3 md:grid-cols-2">
                        <TextField
                          label="제목"
                          required
                          placeholder="예: 약점 보완"
                          value={input.title}
                          onChange={(event) => updatePersonalInput(recordId, "title", event.target.value)}
                        />
                        <div className="flex flex-col gap-2">
                          <label className="text-sm font-medium text-slate-700">
                            내용 <span className="text-rose-500">*</span>
                          </label>
                          <textarea
                            rows={3}
                            placeholder="핵심 학습 내용을 입력하세요."
                            value={input.content}
                            onChange={(event) => updatePersonalInput(recordId, "content", event.target.value)}
                            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200"
                          />
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : null}
          </section>

          <div className="flex justify-end gap-3">
            <Button variant="ghost" onClick={handleRequestClose} disabled={loading}>
              닫기
            </Button>
            <Button onClick={handleSubmit} disabled={loading}>
              {loading ? "저장 중..." : "작성 완료"}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => {
          setConfirmOpen(false);
          resetState();
          onClose();
        }}
        title="작성 중인 내용이 있어요"
        message="작성 중인 내용이 사라집니다. 정말 닫을까요?"
        confirmText="닫기"
        cancelText="계속 작성"
      />
    </>
  );
}
