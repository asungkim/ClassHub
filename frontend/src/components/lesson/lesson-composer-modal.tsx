"use client";

import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import clsx from "clsx";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useLessonComposer, type PersonalLessonFormValues } from "@/contexts/lesson-composer-context";
import { useTeacherCourses } from "@/hooks/api/useTeacherCourses";
import { useCourseStudents } from "@/hooks/api/useCourseStudents";
import { lessonComposerQueryKeys } from "@/hooks/api/lesson-composer-keys";
import type {
  LessonComposerCourse,
  LessonComposerStudent,
  PersonalLessonCreateRequest,
  SharedLessonCreateRequest
} from "@/types/api/lesson";

export function LessonComposerModal() {
  const {
    state,
    resetComposer,
    selectCourse,
    setSharedField,
    setSelectedStudents,
    updatePersonalEntry,
    updateSubmission
  } = useLessonComposer();

  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const [courseSearch, setCourseSearch] = useState("");
  const [studentSearch, setStudentSearch] = useState("");
  const [sharedErrors, setSharedErrors] = useState<{ date?: string; title?: string; content?: string }>({});
  const [personalErrors, setPersonalErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);

  const { data: courses = [], isLoading: coursesLoading } = useTeacherCourses(true);
  const {
    data: students = [],
    isLoading: studentsLoading,
    isFetching: studentsFetching
  } = useCourseStudents(state.selectedCourseId);

  const filteredCourses = useMemo(
    () =>
      courses.filter((course) =>
        (course.name ?? "").toLowerCase().includes(courseSearch.trim().toLowerCase())
      ),
    [courses, courseSearch]
  );

  const normalizedStudents = useMemo(
    () => students.filter((student): student is LessonComposerStudent & { id: string } => Boolean(student.id)),
    [students]
  );

  const filteredStudents = useMemo(
    () =>
      normalizedStudents.filter((student) =>
        (student.name ?? "").toLowerCase().includes(studentSearch.trim().toLowerCase())
      ),
    [normalizedStudents, studentSearch]
  );

  const studentDictionary = useMemo(() => {
    const map = new Map<string, LessonComposerStudent & { id: string }>();
    normalizedStudents.forEach((student) => {
      map.set(student.id, student);
    });
    return map;
  }, [normalizedStudents]);

  const failureMessages = useMemo(() => {
    const map = new Map<string, string>();
    state.submission.failures.forEach((failure) => {
      map.set(failure.studentId, failure.message);
    });
    return map;
  }, [state.submission.failures]);

  const isSubmitting =
    state.submission.status === "creatingShared" || state.submission.status === "creatingPersonal";

  const selectedPersonalEntries = useMemo(
    () =>
      state.selectedStudentIds
        .map((studentId) => state.personalEntries[studentId])
        .filter((entry): entry is PersonalLessonFormValues => Boolean(entry)),
    [state.personalEntries, state.selectedStudentIds]
  );

  const resetValidationState = () => {
    setSharedErrors({});
    setPersonalErrors({});
    setGeneralError(null);
  };

  const handleClose = () => {
    resetValidationState();
    updateSubmission({ status: "idle", failures: [] });
    resetComposer();
  };

  const handleSelectCourse = (course: LessonComposerCourse) => {
    if (!course.id) return;
    selectCourse({ courseId: course.id, courseName: course.name ?? "이름 미지정" });
    setStudentSearch("");
    resetValidationState();
  };

  const handleSharedFieldChange = (field: "date" | "title" | "content", value: string) => {
    setSharedField(field, value);
    setSharedErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const handleStudentToggle = (studentId: string, checked: boolean) => {
    const nextSelected = checked
      ? [...state.selectedStudentIds, studentId]
      : state.selectedStudentIds.filter((id) => id !== studentId);
    setSelectedStudents(nextSelected);
    if (!checked) {
      setPersonalErrors((prev) => {
        if (!prev[studentId]) return prev;
        const next = { ...prev };
        delete next[studentId];
        return next;
      });
    }
  };

  const handlePersonalEntryChange = (
    studentId: string,
    field: "date" | "content",
    value: string
  ) => {
    updatePersonalEntry(studentId, { studentProfileId: studentId, [field]: value });
    setPersonalErrors((prev) => {
      if (!prev[studentId]) return prev;
      const next = { ...prev };
      delete next[studentId];
      return next;
    });
  };

  const sharedFormDisabled = !state.selectedCourseId;

  const validateForms = () => {
    if (!state.selectedCourseId) {
      setGeneralError("반을 먼저 선택해주세요.");
      showToast("error", "반을 선택한 뒤 공통 진도를 작성해야 합니다.");
      return false;
    }

    const shared: { date?: string; title?: string; content?: string } = {};
    if (!state.sharedLessonForm.date) {
      shared.date = "날짜를 선택하세요.";
    }
    if (!state.sharedLessonForm.title?.trim()) {
      shared.title = "제목을 입력하세요.";
    }
    if (!state.sharedLessonForm.content?.trim()) {
      shared.content = "내용을 입력하세요.";
    }
    setSharedErrors(shared);

    const personal: Record<string, string> = {};
    state.selectedStudentIds.forEach((studentId) => {
      const entry = state.personalEntries[studentId];
      if (entry && !entry.content.trim()) {
        personal[studentId] = "개인 진도 내용을 입력하세요.";
      }
    });
    setPersonalErrors(personal);

    const hasSharedError = Object.keys(shared).length > 0;
    const hasPersonalError = Object.keys(personal).length > 0;
    if (hasSharedError || hasPersonalError) {
      setGeneralError("필수 항목을 모두 입력한 뒤 다시 시도해주세요.");
      showToast("error", "필수 입력값을 확인해주세요.");
      return false;
    }

    setGeneralError(null);
    return true;
  };

  const handleSubmit = async () => {
    if (isSubmitting) return;
    if (!validateForms()) {
      return;
    }

    updateSubmission({ status: "creatingShared", failures: [] });

    const sharedBody: SharedLessonCreateRequest = {
      courseId: state.selectedCourseId!,
      date: state.sharedLessonForm.date,
      title: state.sharedLessonForm.title.trim(),
      content: state.sharedLessonForm.content.trim()
    };

    const targetCourseId = state.selectedCourseId;

    try {
      const response = await api.POST("/api/v1/shared-lessons", {
        body: sharedBody
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      if (selectedPersonalEntries.length > 0) {
        updateSubmission({ status: "creatingPersonal", failures: [] });
        const fallbackDate = sharedBody.date ?? new Date().toISOString().split("T")[0];
        const personalResults = await Promise.allSettled(
          selectedPersonalEntries.map(async (entry) => {
            const personalBody: PersonalLessonCreateRequest = {
              studentProfileId: entry.studentProfileId,
              date: entry.date || fallbackDate,
              content: entry.content.trim()
            };
            const personalResponse = await api.POST("/api/v1/personal-lessons", {
              body: personalBody
            });
            const personalError = getFetchError(personalResponse);
            if (personalError) {
              throw personalError;
            }
            return personalResponse.data?.data;
          })
        );

        const failures = personalResults
          .map((result, index) => {
            if (result.status === "fulfilled") {
              return null;
            }
            const entry = selectedPersonalEntries[index];
            return {
              studentId: entry.studentProfileId,
              message: getApiErrorMessage(result.reason, "개인 진도 작성에 실패했습니다.")
            };
          })
          .filter(Boolean) as { studentId: string; message: string }[];

        if (failures.length > 0) {
          updateSubmission({ status: "error", failures });
          const failureErrors: Record<string, string> = {};
          failures.forEach((failure) => {
            failureErrors[failure.studentId] = failure.message;
          });
          setPersonalErrors((prev) => ({ ...prev, ...failureErrors }));
          showToast("error", `개인 진도 ${failures.length}건이 실패했습니다. 메시지를 확인하세요.`);
          return;
        }
      }

      updateSubmission({ status: "success", failures: [] });
      showToast(
        "success",
        selectedPersonalEntries.length > 0
          ? `공통 진도와 개인 진도 ${selectedPersonalEntries.length}건을 저장했습니다.`
          : "공통 진도를 저장했습니다."
      );

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["student-calendar"] }),
        queryClient.invalidateQueries({ queryKey: lessonComposerQueryKeys.courses(true) }),
        targetCourseId
          ? queryClient.invalidateQueries({
              queryKey: lessonComposerQueryKeys.courseStudents(targetCourseId)
            })
          : Promise.resolve()
      ]);

      handleClose();
    } catch (error) {
      updateSubmission({ status: "error" });
      const message = getApiErrorMessage(error, "수업 내용 작성에 실패했습니다.");
      setGeneralError(message);
      showToast("error", message);
    }
  };

  return (
    <Modal
      open={state.isOpen}
      onClose={handleClose}
      title="수업 내용 작성"
      size="lg"
      className="max-w-4xl"
      mobileLayout="bottom-sheet"
    >
      <div className="relative">
        <div className="space-y-8 pb-28">
          <CourseSelectSection
            courses={filteredCourses}
            isLoading={coursesLoading}
            isDisabled={isSubmitting}
            searchValue={courseSearch}
            onSearchChange={setCourseSearch}
            onSelectCourse={handleSelectCourse}
            selectedCourseId={state.selectedCourseId}
          />

          <SharedLessonFormSection
            disabled={sharedFormDisabled || isSubmitting}
            date={state.sharedLessonForm.date}
            title={state.sharedLessonForm.title}
            content={state.sharedLessonForm.content}
            onChangeField={handleSharedFieldChange}
            errors={sharedErrors}
          />

          <StudentSelectSection
            disabled={sharedFormDisabled}
            students={filteredStudents}
            isLoading={studentsLoading}
            isFetching={studentsFetching}
            searchValue={studentSearch}
            onSearchChange={setStudentSearch}
            selectedStudentIds={state.selectedStudentIds}
            onToggleStudent={handleStudentToggle}
            isSubmitting={isSubmitting}
          />

          <PersonalLessonFormsSection
            selectedIds={state.selectedStudentIds}
            studentDictionary={studentDictionary}
            personalEntries={state.personalEntries}
            onChangeEntry={handlePersonalEntryChange}
            personalErrors={personalErrors}
            failureMessages={failureMessages}
            disabled={isSubmitting}
          />
        </div>

        <div className="sticky bottom-0 -mx-6 mt-6 flex flex-col gap-3 border-t border-slate-200 bg-white/95 px-6 py-4 shadow-[0_-8px_20px_rgba(15,23,42,0.08)]">
          <div className="flex flex-col gap-1 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
            <span className="font-semibold text-slate-700">
              선택 학생 {state.selectedStudentIds.length}명
            </span>
            {generalError ? (
              <span className="text-rose-600">{generalError}</span>
            ) : state.submission.status === "creatingShared" ? (
              <span>공통 진도를 작성하는 중이에요...</span>
            ) : state.submission.status === "creatingPersonal" ? (
              <span>개인 진도 {selectedPersonalEntries.length}건을 작성 중...</span>
            ) : state.submission.failures.length > 0 ? (
              <span className="text-amber-600">
                실패 {state.submission.failures.length}건 - 아래 메시지를 확인하세요.
              </span>
            ) : (
              <span>작성 후 캘린더와 목록이 자동으로 갱신됩니다.</span>
            )}
          </div>

          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button
              variant="ghost"
              onClick={handleClose}
              disabled={isSubmitting}
              className="sm:min-w-[120px]"
            >
              취소
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={isSubmitting || sharedFormDisabled}
              className="sm:min-w-[160px]"
            >
              {isSubmitting
                ? state.submission.status === "creatingShared"
                  ? "공통 진도 작성 중..."
                  : "개인 진도 작성 중..."
                : "작성 완료"}
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
}

type CourseSelectSectionProps = {
  courses: LessonComposerCourse[];
  isLoading: boolean;
  isDisabled?: boolean;
  searchValue: string;
  onSearchChange: (value: string) => void;
  onSelectCourse: (course: LessonComposerCourse) => void;
  selectedCourseId?: string;
};

function CourseSelectSection({
  courses,
  isLoading,
  isDisabled = false,
  searchValue,
  onSearchChange,
  onSelectCourse,
  selectedCourseId
}: CourseSelectSectionProps) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white/70 p-6 shadow-sm">
      <div className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Phase 1</p>
            <h3 className="text-xl font-bold text-slate-900">반을 선택하세요</h3>
            <p className="text-sm text-slate-500">활성화된 반만 표시됩니다.</p>
          </div>
          <TextField
            type="search"
            placeholder="반 이름 검색"
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-64 max-w-full"
            aria-label="반 검색"
            disabled={isDisabled}
          />
        </div>

        <div className="mt-4 max-h-56 overflow-y-auto rounded-2xl border border-slate-100 bg-white shadow-inner">
          {isLoading ? (
            <div className="space-y-3 p-4">
              <Skeleton className="h-11 w-full rounded-2xl" />
              <Skeleton className="h-11 w-full rounded-2xl" />
              <Skeleton className="h-11 w-full rounded-2xl" />
            </div>
          ) : courses.length === 0 ? (
            <div className="p-6 text-sm text-slate-500">조건에 맞는 반이 없습니다.</div>
          ) : (
            <ul className="divide-y divide-slate-100">
              {courses.map((course) => {
                if (!course.id) {
                  return null;
                }
                const isSelected = course.id === selectedCourseId;
                return (
                  <li key={course.id}>
                    <button
                      type="button"
                      onClick={() => onSelectCourse(course)}
                      disabled={isDisabled}
                      className={clsx(
                        "flex w-full items-center justify-between px-5 py-3 text-left transition",
                        isSelected
                          ? "bg-blue-50 text-blue-700"
                          : "hover:bg-slate-50 text-slate-700",
                        isDisabled && "cursor-not-allowed opacity-50"
                      )}
                    >
                      <div>
                        <p className="text-base font-semibold">{course.name ?? "이름 미지정"}</p>
                        <p className="text-xs text-slate-500">{course.company ?? "소속 정보 없음"}</p>
                      </div>
                      <span
                        className={clsx(
                          "rounded-full px-3 py-1 text-xs font-semibold",
                          course.isActive ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"
                        )}
                      >
                        {course.isActive ? "운영중" : "비활성"}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}

type SharedLessonFormSectionProps = {
  disabled: boolean;
  date: string;
  title: string;
  content: string;
  onChangeField: (field: "date" | "title" | "content", value: string) => void;
  errors: { date?: string; title?: string; content?: string };
};

function SharedLessonFormSection({
  disabled,
  date,
  title,
  content,
  onChangeField,
  errors
}: SharedLessonFormSectionProps) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white/70 p-6 shadow-sm">
      <div className="flex flex-col gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Phase 2</p>
          <h3 className="text-xl font-bold text-slate-900">공통 진도를 작성하세요</h3>
          <p className="text-sm text-slate-500">반을 선택하면 날짜/제목/내용을 입력할 수 있습니다.</p>
        </div>

        <div className={clsx("grid gap-4 md:grid-cols-2", disabled && "opacity-60")}>
          <TextField
            type="date"
            label="작성 날짜"
            required
            value={date}
            onChange={(e) => onChangeField("date", e.target.value)}
            disabled={disabled}
            error={errors.date}
          />
          <TextField
            label="제목"
            placeholder="예: 1단원 개념 복습"
            required
            value={title}
            onChange={(e) => onChangeField("title", e.target.value)}
            disabled={disabled}
            error={errors.title}
          />
        </div>

        <label className={clsx("flex flex-col gap-2", disabled && "opacity-60")}>
          <span className="text-sm font-semibold text-slate-700">
            내용 <span className="text-rose-500">*</span>
          </span>
          <textarea
            placeholder="수업 내용을 자세히 입력하세요"
            rows={5}
            className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-900 shadow-inner focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-50"
            value={content}
            onChange={(e) => onChangeField("content", e.target.value)}
            disabled={disabled}
          />
          {errors.content ? (
            <span className="text-xs font-semibold text-rose-600">{errors.content}</span>
          ) : null}
        </label>
      </div>
    </section>
  );
}

type StudentSelectSectionProps = {
  disabled: boolean;
  students: Array<LessonComposerStudent & { id: string }>;
  isLoading: boolean;
  isFetching: boolean;
  searchValue: string;
  onSearchChange: (value: string) => void;
  selectedStudentIds: string[];
  onToggleStudent: (studentId: string, checked: boolean) => void;
  isSubmitting: boolean;
};

function StudentSelectSection({
  disabled,
  students,
  isLoading,
  isFetching,
  searchValue,
  onSearchChange,
  selectedStudentIds,
  onToggleStudent,
  isSubmitting
}: StudentSelectSectionProps) {
  const isDisabled = disabled || isSubmitting;
  return (
    <section className="rounded-3xl border border-slate-200 bg-white/70 p-6 shadow-sm">
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Phase 3</p>
            <h3 className="text-xl font-bold text-slate-900">학생을 선택하세요</h3>
            <p className="text-sm text-slate-500">선택된 학생 수 만큼 개인 진도 폼이 생성됩니다.</p>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm font-semibold text-blue-600">
              선택 {selectedStudentIds.length}명
            </span>
            <TextField
              type="search"
              placeholder="학생 검색"
              value={searchValue}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-48"
              disabled={isDisabled}
              aria-label="학생 검색"
            />
          </div>
        </div>

        {disabled ? (
          <div className="rounded-2xl border border-dashed border-slate-200 p-6 text-sm text-slate-500">
            반을 먼저 선택하면 학생 목록이 나타납니다.
          </div>
        ) : isSubmitting ? (
          <div className="rounded-2xl border border-slate-100 bg-white/70 p-6 text-sm text-slate-500">
            작성 중에는 학생 선택을 변경할 수 없습니다.
          </div>
        ) : (
          <div className="rounded-2xl border border-slate-100 bg-white shadow-inner">
            {isLoading ? (
              <div className="space-y-3 p-4">
                <Skeleton className="h-6 w-full rounded-2xl" />
                <Skeleton className="h-6 w-full rounded-2xl" />
                <Skeleton className="h-6 w-full rounded-2xl" />
              </div>
            ) : students.length === 0 ? (
              <div className="p-6 text-sm text-slate-500">반에 등록된 학생이 없습니다.</div>
            ) : (
              <ul className="max-h-48 space-y-3 overflow-y-auto p-4">
                {students.map((student) => {
                  const checked = selectedStudentIds.includes(student.id);
                  const normalizedName = (student.name ?? "").toLowerCase();
                  if (
                    searchValue &&
                    !normalizedName.includes(searchValue.trim().toLowerCase())
                  ) {
                    return null;
                  }
                  const studentName = student.name ?? "이름 미상";
                  return (
                    <li
                      key={student.id}
                      className="flex items-center justify-between rounded-2xl border border-slate-100 px-4 py-2"
                    >
                      <Checkbox
                        label={
                          <div>
                            <p className="text-sm font-semibold text-slate-800">{studentName}</p>
                            <p className="text-xs text-slate-500">{student.phoneNumber ?? "연락처 미등록"}</p>
                          </div>
                        }
                        checked={checked}
                        onChange={(e) => onToggleStudent(student.id, e.target.checked)}
                        disabled={isDisabled}
                      />
                      {checked ? (
                        <span className="text-xs text-blue-600">선택됨</span>
                      ) : (
                        <span className="text-xs text-slate-400">{isFetching ? "업데이트 중..." : ""}</span>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

type PersonalLessonFormsSectionProps = {
  selectedIds: string[];
  studentDictionary: Map<string, LessonComposerStudent & { id: string }>;
  personalEntries: Record<string, PersonalLessonFormValues>;
  onChangeEntry: (studentId: string, field: "date" | "content", value: string) => void;
  personalErrors: Record<string, string>;
  failureMessages: Map<string, string>;
  disabled: boolean;
};

function PersonalLessonFormsSection({
  selectedIds,
  studentDictionary,
  personalEntries,
  onChangeEntry,
  personalErrors,
  failureMessages,
  disabled
}: PersonalLessonFormsSectionProps) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white/70 p-6 shadow-sm">
      <div className="flex flex-col gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Phase 4</p>
          <h3 className="text-xl font-bold text-slate-900">선택한 학생의 개인 진도를 작성하세요</h3>
          <p className="text-sm text-slate-500">
            선택하지 않은 학생은 개인 진도가 생성되지 않습니다. 필요한 학생만 선택하세요.
          </p>
        </div>

        {selectedIds.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 p-6 text-sm text-slate-500">
            학생을 선택하면 개인 진도 입력 폼이 나타납니다.
          </div>
        ) : (
          <div className="space-y-4">
            {selectedIds.map((studentId) => {
              const student = studentDictionary.get(studentId);
              const entry = personalEntries[studentId];
              if (!student || !entry) {
                return null;
              }
              const entryError = personalErrors[studentId];
              const failureMessage = failureMessages.get(studentId);
              const studentName = student.name ?? "이름 미상";
              return (
                <article
                  key={studentId}
                  className={clsx(
                    "rounded-2xl border bg-white p-4 shadow-inner",
                    entryError || failureMessage ? "border-rose-200" : "border-slate-100"
                  )}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-base font-semibold text-slate-900">{studentName}</p>
                      <p className="text-xs text-slate-500">{student.phoneNumber ?? "연락처 미등록"}</p>
                    </div>
                    <span className="text-xs font-semibold text-slate-400">학생 ID: {studentId}</span>
                  </div>

                  <div className="mt-3 grid gap-3 md:grid-cols-2">
                    <TextField
                      type="date"
                      label="진도 날짜"
                      value={entry.date}
                      onChange={(e) => onChangeEntry(studentId, "date", e.target.value)}
                      disabled={disabled}
                    />
                  </div>

                  <label className="mt-3 flex flex-col gap-2">
                    <span className="text-sm font-semibold text-slate-700">
                      개인 진도 내용 <span className="text-rose-500">*</span>
                    </span>
                    <textarea
                      rows={4}
                      className={clsx(
                        "w-full rounded-2xl border px-4 py-3 text-sm text-slate-900 shadow-inner focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-50",
                        entryError && "border-rose-300 focus:border-rose-400 focus:ring-rose-100"
                      )}
                      placeholder="학생별 메모, 과제, 피드백 등을 입력하세요"
                      value={entry.content}
                      onChange={(e) => onChangeEntry(studentId, "content", e.target.value)}
                      disabled={disabled}
                    />
                    {entryError ? (
                      <span className="text-xs font-semibold text-rose-600">{entryError}</span>
                    ) : failureMessage ? (
                      <span className="text-xs font-semibold text-amber-600">{failureMessage}</span>
                    ) : null}
                  </label>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
