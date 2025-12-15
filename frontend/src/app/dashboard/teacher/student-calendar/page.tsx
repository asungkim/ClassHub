"use client";

import { Suspense, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useStudentProfiles, useStudentCalendar } from "@/hooks/use-student-calendar";
import { useStudentProfileDetail } from "@/hooks/use-student-profiles";
import {
  useDeletePersonalLesson,
  useDeleteSharedLesson,
  useUpdatePersonalLesson,
  useUpdateSharedLesson
} from "@/hooks/use-lesson-mutations";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { TextField } from "@/components/ui/text-field";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EditLessonModal } from "@/components/lesson/edit-lesson-modal";
import { useToast } from "@/components/ui/toast";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type StudentProfileSummary = components["schemas"]["StudentProfileSummary"];
type StudentCalendarResponse = components["schemas"]["StudentCalendarResponse"];
type CalendarSharedLessonDto = components["schemas"]["CalendarSharedLessonDto"];
type CalendarPersonalLessonDto = components["schemas"]["CalendarPersonalLessonDto"];
type CalendarClinicRecordDto = components["schemas"]["CalendarClinicRecordDto"];

// Next.js 동적 렌더링 강제
export const dynamic = "force-dynamic";

// 범례 아이템 정의
const LEGEND_ITEMS = [
  { type: "shared", label: "공통 진도", color: "#0A63FF" },
  { type: "personal", label: "개인 진도", color: "#00A86B" },
  { type: "clinic", label: "클리닉 기록", color: "#F2B705" }
] as const;

// 요일 헤더 (월요일 시작)
const WEEKDAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

// 날짜별 데이터 집계 타입
type CalendarDayData = {
  date: string; // YYYY-MM-DD
  dayOfMonth: number;
  isCurrentMonth: boolean;
  sharedLessons: CalendarSharedLessonDto[];
  personalLessons: CalendarPersonalLessonDto[];
  clinicRecords: CalendarClinicRecordDto[];
};

// 캘린더 매트릭스 생성 (7열 x 주차)
function buildCalendarMatrix(year: number, month: number, calendarData?: StudentCalendarResponse): CalendarDayData[][] {
  const firstDay = new Date(year, month - 1, 1);
  const lastDay = new Date(year, month, 0);
  const daysInMonth = lastDay.getDate();

  // 월요일 시작으로 조정: getDay()는 일요일=0, 월요일=1
  let firstDayOfWeek = firstDay.getDay();
  firstDayOfWeek = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1; // 월요일=0으로 변환

  // 데이터를 날짜별로 매핑
  const dataByDate = new Map<string, CalendarDayData>();

  if (calendarData) {
    calendarData.sharedLessons?.forEach((lesson) => {
      if (lesson.date) {
        if (!dataByDate.has(lesson.date)) {
          const d = new Date(lesson.date);
          dataByDate.set(lesson.date, {
            date: lesson.date,
            dayOfMonth: d.getDate(),
            isCurrentMonth: true,
            sharedLessons: [],
            personalLessons: [],
            clinicRecords: []
          });
        }
        dataByDate.get(lesson.date)!.sharedLessons.push(lesson);
      }
    });

    calendarData.personalLessons?.forEach((lesson) => {
      if (lesson.date) {
        if (!dataByDate.has(lesson.date)) {
          const d = new Date(lesson.date);
          dataByDate.set(lesson.date, {
            date: lesson.date,
            dayOfMonth: d.getDate(),
            isCurrentMonth: true,
            sharedLessons: [],
            personalLessons: [],
            clinicRecords: []
          });
        }
        dataByDate.get(lesson.date)!.personalLessons.push(lesson);
      }
    });

    calendarData.clinicRecords?.forEach((record) => {
      if (record.date) {
        if (!dataByDate.has(record.date)) {
          const d = new Date(record.date);
          dataByDate.set(record.date, {
            date: record.date,
            dayOfMonth: d.getDate(),
            isCurrentMonth: true,
            sharedLessons: [],
            personalLessons: [],
            clinicRecords: []
          });
        }
        dataByDate.get(record.date)!.clinicRecords.push(record);
      }
    });
  }

  // 캘린더 셀 배열 생성
  const cells: CalendarDayData[] = [];

  // 이전 달 날짜로 앞부분 채우기
  const prevMonthLastDay = new Date(year, month - 1, 0).getDate();
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const day = prevMonthLastDay - i;
    const prevMonth = month === 1 ? 12 : month - 1;
    const prevYear = month === 1 ? year - 1 : year;
    const dateStr = `${prevYear}-${String(prevMonth).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    cells.push({
      date: dateStr,
      dayOfMonth: day,
      isCurrentMonth: false,
      sharedLessons: [],
      personalLessons: [],
      clinicRecords: []
    });
  }

  // 현재 달 날짜
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    if (dataByDate.has(dateStr)) {
      cells.push(dataByDate.get(dateStr)!);
    } else {
      cells.push({
        date: dateStr,
        dayOfMonth: day,
        isCurrentMonth: true,
        sharedLessons: [],
        personalLessons: [],
        clinicRecords: []
      });
    }
  }

  // 다음 달 날짜로 뒷부분 채우기 (7의 배수로 맞추기)
  const remainingCells = 7 - (cells.length % 7);
  if (remainingCells < 7) {
    const nextMonth = month === 12 ? 1 : month + 1;
    const nextYear = month === 12 ? year + 1 : year;
    for (let day = 1; day <= remainingCells; day++) {
      const dateStr = `${nextYear}-${String(nextMonth).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
      cells.push({
        date: dateStr,
        dayOfMonth: day,
        isCurrentMonth: false,
        sharedLessons: [],
        personalLessons: [],
        clinicRecords: []
      });
    }
  }

  // 7열씩 나누어 주차별 배열 생성
  const matrix: CalendarDayData[][] = [];
  for (let i = 0; i < cells.length; i += 7) {
    matrix.push(cells.slice(i, i + 7));
  }

  return matrix;
}

// 날짜 셀의 색상 바 생성
function getBarsForDay(dayData: CalendarDayData): Array<{ color: string; count: number }> {
  const bars: Array<{ color: string; count: number }> = [];

  if (dayData.sharedLessons.length > 0) {
    bars.push({ color: "#0A63FF", count: dayData.sharedLessons.length });
  }
  if (dayData.personalLessons.length > 0) {
    bars.push({ color: "#00A86B", count: dayData.personalLessons.length });
  }
  if (dayData.clinicRecords.length > 0) {
    bars.push({ color: "#F2B705", count: dayData.clinicRecords.length });
  }

  return bars;
}

function StudentCalendarContent() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const router = useRouter();
  const searchParams = useSearchParams();

  // URL에서 studentId, year, month 가져오기
  const studentIdFromUrl = searchParams?.get("studentId") ?? undefined;
  const yearFromUrl = searchParams?.get("year");
  const monthFromUrl = searchParams?.get("month");

  // 학생 검색 상태
  const [searchName, setSearchName] = useState("");
  const [selectedStudent, setSelectedStudent] = useState<StudentProfileSummary | null>(null);
  const [showSearchResults, setShowSearchResults] = useState(false);

  // 모달 상태
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedDayData, setSelectedDayData] = useState<CalendarDayData | null>(null);

  // 삭제 확인 다이얼로그 상태
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingItem, setDeletingItem] = useState<{
    type: "shared" | "personal";
    id: string;
  } | null>(null);

  // 수정 상태
  const [editingItem, setEditingItem] = useState<{
    type: "shared" | "personal";
    id: string;
    data: { title?: string; content?: string };
  } | null>(null);

  // 월 상태 (초기값: URL 또는 현재 날짜)
  const now = new Date();
  const [year, setYear] = useState(yearFromUrl ? parseInt(yearFromUrl, 10) : now.getFullYear());
  const [month, setMonth] = useState(monthFromUrl ? parseInt(monthFromUrl, 10) : now.getMonth() + 1);

  // 학생 검색 훅 (디바운스 적용)
  const { data: searchResults = [], isLoading: isSearching } = useStudentProfiles(searchName);
  const selectedStudentId = selectedStudent?.id;
  const { data: selectedStudentDetail } = useStudentProfileDetail(selectedStudentId ?? "");

  // 캘린더 데이터 조회 훅
  const {
    data: calendarData,
    isLoading: isCalendarLoading,
    error: calendarError,
    refetch: refetchCalendar
  } = useStudentCalendar(selectedStudent?.id, { year, month });

  // 뮤테이션 훅
  const deleteSharedLesson = useDeleteSharedLesson();
  const deletePersonalLesson = useDeletePersonalLesson();
  const updateSharedLesson = useUpdateSharedLesson();
  const updatePersonalLesson = useUpdatePersonalLesson();

  // Toast 훅
  const { showToast } = useToast();
  const detailedCourseNames = useMemo(() => {
    if (!selectedStudentDetail?.enrolledCourses?.length) {
      return undefined;
    }
    return selectedStudentDetail.enrolledCourses
      .map((course) => course.courseName)
      .filter((name): name is string => Boolean(name && name.trim()));
  }, [selectedStudentDetail]);
  const selectedCourseNames = detailedCourseNames?.length
    ? detailedCourseNames
    : selectedStudent?.courseNames;

  // 캘린더 매트릭스 생성 (가드 여부와 무관하게 훅 순서 유지)
  const calendarMatrix = useMemo(
    () => buildCalendarMatrix(year, month, calendarData ?? undefined),
    [year, month, calendarData]
  );

  if (!canRender) {
    return fallback;
  }

  const handleSelectStudent = (student: StudentProfileSummary) => {
    setSelectedStudent(student);
    setSearchName(student.name ?? "");
    setShowSearchResults(false);

    // URL에 studentId 추가
    if (student.id) {
      const params = new URLSearchParams();
      params.set("studentId", student.id);
      params.set("year", String(year));
      params.set("month", String(month));
      router.push(`/dashboard/teacher/student-calendar?${params.toString()}`);
    }
  };

  const handleClearStudent = () => {
    setSelectedStudent(null);
    setSearchName("");
    // URL에서 studentId 제거
    router.push("/dashboard/teacher/student-calendar");
  };

  // 월 이동 핸들러 (상태만 변경, URL 업데이트 없음)
  const handlePrevMonth = () => {
    const newDate = new Date(year, month - 2); // month는 1-based, Date는 0-based
    const newYear = newDate.getFullYear();
    const newMonth = newDate.getMonth() + 1;
    setYear(newYear);
    setMonth(newMonth);
    // React Query가 자동으로 새 데이터를 가져옴 (queryKey 변경)
  };

  const handleNextMonth = () => {
    const newDate = new Date(year, month); // month는 1-based, Date는 0-based
    const newYear = newDate.getFullYear();
    const newMonth = newDate.getMonth() + 1;
    setYear(newYear);
    setMonth(newMonth);
    // React Query가 자동으로 새 데이터를 가져옴 (queryKey 변경)
  };

  // 월 레이블 포맷팅
  const monthLabel = `${year}년 ${month}월`;

  // 날짜 클릭 핸들러
  const handleDayClick = (dayData: CalendarDayData) => {
    if (!dayData.isCurrentMonth) return;
    setSelectedDayData(dayData);
    setModalOpen(true);
  };

  const handleCloseModal = () => {
    setModalOpen(false);
    setSelectedDayData(null);
  };

  // 삭제 핸들러
  const handleDeleteClick = (type: "shared" | "personal", id: string) => {
    setDeletingItem({ type, id });
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingItem) return;

    try {
      if (deletingItem.type === "shared") {
        await deleteSharedLesson.mutateAsync(deletingItem.id);
      } else {
        await deletePersonalLesson.mutateAsync(deletingItem.id);
      }
      // 삭제 성공 시 모달 닫기
      setDeleteDialogOpen(false);
      setDeletingItem(null);
      setModalOpen(false);
      setSelectedDayData(null);
      showToast("success", "삭제되었습니다.");
    } catch (error) {
      const errorMessage = getApiErrorMessage(error, "삭제 중 오류가 발생했습니다.");
      showToast("error", errorMessage);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setDeletingItem(null);
  };

  // 수정 핸들러
  const handleEditClick = (
    type: "shared" | "personal",
    id: string,
    currentData: { title?: string; content?: string }
  ) => {
    setEditingItem({ type, id, data: currentData });
  };

  const handleEditSave = async (data: { title?: string; content?: string }) => {
    if (!editingItem) return;

    try {
      if (editingItem.type === "shared") {
        await updateSharedLesson.mutateAsync({ id: editingItem.id, data });
      } else {
        await updatePersonalLesson.mutateAsync({ id: editingItem.id, data });
      }
      // 수정 성공 시 모달 닫기
      setEditingItem(null);
      setModalOpen(false);
      setSelectedDayData(null);
      showToast("success", "수정되었습니다.");
    } catch (error) {
      const errorMessage = getApiErrorMessage(error, "수정 중 오류가 발생했습니다.");
      showToast("error", errorMessage);
      // 에러 발생 시 수정 모달은 유지 (사용자가 다시 시도할 수 있도록)
    }
  };

  const handleEditCancel = () => {
    setEditingItem(null);
  };

  return (
    <DashboardShell
      title="학생별 캘린더"
      subtitle="학생별 월간 학습 데이터를 조회하고 관리할 수 있습니다."
    >
      <div className="space-y-6">
        {/* 학생 검색 영역 */}
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="relative">
            <TextField
              label="학생 검색"
              placeholder="학생 이름을 입력하세요 (최소 1자)"
              value={searchName}
              onChange={(e) => {
                setSearchName(e.target.value);
                setShowSearchResults(true);
              }}
              onFocus={() => setShowSearchResults(true)}
              icon={
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                  />
                </svg>
              }
            />

            {/* 검색 결과 드롭다운 */}
            {showSearchResults && searchName.trim().length >= 1 && (
              <div className="absolute z-10 mt-2 w-full rounded-xl border border-slate-200 bg-white shadow-lg">
                {isSearching ? (
                  <div className="p-4 text-center text-sm text-slate-500">검색 중...</div>
                ) : searchResults.length === 0 ? (
                  <div className="p-4 text-center text-sm text-slate-500">검색 결과가 없습니다.</div>
                ) : (
                  <ul className="max-h-60 overflow-y-auto">
                    {searchResults.map((student) => (
                      <li key={student.id}>
                        <button
                          onClick={() => handleSelectStudent(student)}
                          className="w-full px-4 py-3 text-left hover:bg-slate-50 transition flex items-center justify-between"
                        >
                          <div>
                            <div className="font-medium text-slate-900">{student.name}</div>
                            <div className="text-xs text-slate-500">
                              {formatCourseNames(student.courseNames)} • {student.grade ?? ""}
                            </div>
                          </div>
                          {student.active !== false && (
                            <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-semibold text-blue-700">
                              활성
                            </span>
                          )}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 선택된 학생 카드 */}
        {selectedStudent ? (
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-start justify-between">
              <div className="space-y-2">
                <h3 className="text-lg font-semibold text-slate-900">{selectedStudent.name}</h3>
                <div className="space-y-1 text-sm text-slate-600">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">반:</span>
                    <span>{formatCourseNames(selectedCourseNames)}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">연락처:</span>
                    <span>{selectedStudent.phoneNumber ?? "-"}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">상태:</span>
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                        selectedStudent.active !== false
                          ? "bg-blue-100 text-blue-700"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {selectedStudent.active !== false ? "활성" : "비활성"}
                    </span>
                  </div>
                </div>
              </div>
              <button
                onClick={handleClearStudent}
                className="rounded-lg px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 transition"
              >
                변경
              </button>
            </div>
          </div>
        ) : (
          /* 빈 상태 */
          <div className="rounded-2xl border border-slate-200 bg-white p-12 shadow-sm">
            <EmptyState
              message="학생을 선택해주세요"
              description="위 검색창에서 학생 이름을 입력하여 선택하세요"
              icon={
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-12 w-12"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                  />
                </svg>
              }
            />
          </div>
        )}

        {/* 캘린더 영역 */}
        {selectedStudent && (
          <div className="space-y-4">
            {/* 범례 */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex flex-wrap gap-4 justify-center md:justify-start">
                {LEGEND_ITEMS.map((item) => (
                  <div key={item.type} className="flex items-center gap-2">
                    <div
                      className="h-4 w-4 rounded"
                      style={{ backgroundColor: item.color }}
                    />
                    <span className="text-sm font-medium text-slate-700">{item.label}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* 월 이동 컨트롤 */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex items-center justify-center gap-4">
                <button
                  onClick={handlePrevMonth}
                  className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50 transition"
                  aria-label="이전 달"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-5 w-5 text-slate-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                <span className="text-lg font-semibold text-slate-900 min-w-[140px] text-center">
                  {monthLabel}
                </span>
                <button
                  onClick={handleNextMonth}
                  className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50 transition"
                  aria-label="다음 달"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-5 w-5 text-slate-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
            </div>

            {/* 캘린더 그리드 */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              {isCalendarLoading ? (
                <div className="text-center text-slate-500 py-12">로딩 중...</div>
              ) : calendarError ? (
                <div className="text-center py-12">
                  <p className="text-red-600 mb-4">데이터를 불러오는 중 오류가 발생했습니다.</p>
                  <button
                    onClick={() => refetchCalendar()}
                    className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition"
                  >
                    다시 시도
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  {/* 요일 헤더 */}
                  <div className="grid grid-cols-7 gap-2">
                    {WEEKDAY_LABELS.map((label) => (
                      <div
                        key={label}
                        className="text-center text-sm font-semibold text-slate-600 py-2"
                      >
                        {label}
                      </div>
                    ))}
                  </div>

                  {/* 날짜 그리드 */}
                  {calendarMatrix.map((week, weekIndex) => (
                    <div key={weekIndex} className="grid grid-cols-7 gap-2">
                      {week.map((dayData) => {
                        const bars = getBarsForDay(dayData);
                        const totalEvents =
                          dayData.sharedLessons.length +
                          dayData.personalLessons.length +
                          dayData.clinicRecords.length;
                        const hasOverflow = totalEvents > 3;
                        const displayBars = bars.slice(0, 3);

                        return (
                          <button
                            key={dayData.date}
                            onClick={() => handleDayClick(dayData)}
                            className={`relative min-h-[100px] rounded-lg border p-2 text-left transition hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                              dayData.isCurrentMonth
                                ? "border-slate-200 bg-white hover:bg-slate-50"
                                : "border-slate-100 bg-slate-50 text-slate-400"
                            }`}
                            disabled={!dayData.isCurrentMonth}
                          >
                            {/* 날짜 숫자 - 왼쪽 위 */}
                            <div
                              className={`absolute top-2 left-2 text-sm font-semibold ${
                                dayData.isCurrentMonth ? "text-slate-900" : "text-slate-400"
                              }`}
                            >
                              {dayData.dayOfMonth}
                            </div>

                            {/* 이벤트 바 */}
                            {dayData.isCurrentMonth && displayBars.length > 0 && (
                              <div className="mt-7 space-y-1">
                                {displayBars.map((bar, idx) => (
                                  <div
                                    key={idx}
                                    className="h-1.5 rounded-full"
                                    style={{ backgroundColor: bar.color }}
                                  />
                                ))}
                              </div>
                            )}

                            {/* 오버플로 인디케이터 */}
                            {dayData.isCurrentMonth && hasOverflow && (
                              <div className="absolute bottom-2 right-2 text-xs font-semibold text-slate-500">
                                +{totalEvents - 3}
                              </div>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  ))}

                  {/* Empty State */}
                  {calendarMatrix.every((week) =>
                    week.every(
                      (day) =>
                        day.sharedLessons.length === 0 &&
                        day.personalLessons.length === 0 &&
                        day.clinicRecords.length === 0
                    )
                  ) && (
                    <div className="text-center py-8 text-slate-500">
                      <p className="text-sm">해당 월에는 기록이 없습니다.</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* 날짜 상세 모달 */}
      {selectedDayData && (
        <Modal
          open={modalOpen}
          onClose={handleCloseModal}
          title={`${year}년 ${month}월 ${selectedDayData.dayOfMonth}일`}
          size="lg"
        >
          <div className="space-y-6">
            {/* SharedLesson 섹션 */}
            {selectedDayData.sharedLessons.length > 0 && (
              <div>
                <h3 className="text-lg font-semibold text-slate-900 mb-3">공통 진도</h3>
                <div className="space-y-3">
                  {selectedDayData.sharedLessons.map((lesson) => (
                    <div
                      key={lesson.id}
                      className="rounded-lg border border-blue-200 bg-blue-50 p-4"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2">
                            <span className="rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
                              {lesson.courseName}
                            </span>
                          </div>
                          <h4 className="font-semibold text-slate-900 mb-1">{lesson.title}</h4>
                          <p className="text-sm text-slate-600">{lesson.content}</p>
                        </div>
                        {lesson.editable && lesson.id && (
                          <div className="flex gap-2 ml-4">
                            <button
                              onClick={() =>
                                handleEditClick("shared", lesson.id!, {
                                  title: lesson.title,
                                  content: lesson.content
                                })
                              }
                              className="rounded-lg bg-white px-3 py-1.5 text-sm font-medium text-slate-700 border border-slate-200 hover:bg-slate-50 transition"
                            >
                              수정
                            </button>
                            <button
                              onClick={() => handleDeleteClick("shared", lesson.id!)}
                              className="rounded-lg bg-white px-3 py-1.5 text-sm font-medium text-red-600 border border-red-200 hover:bg-red-50 transition"
                            >
                              삭제
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* PersonalLesson 섹션 */}
            {selectedDayData.personalLessons.length > 0 && (
              <div>
                <h3 className="text-lg font-semibold text-slate-900 mb-3">개인 진도</h3>
                <div className="space-y-3">
                  {selectedDayData.personalLessons.map((lesson) => (
                    <div
                      key={lesson.id}
                      className="rounded-lg border border-green-200 bg-green-50 p-4"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <p className="text-sm text-slate-600">{lesson.content}</p>
                        </div>
                        {lesson.editable && lesson.id && (
                          <div className="flex gap-2 ml-4">
                            <button
                              onClick={() =>
                                handleEditClick("personal", lesson.id!, {
                                  content: lesson.content
                                })
                              }
                              className="rounded-lg bg-white px-3 py-1.5 text-sm font-medium text-slate-700 border border-slate-200 hover:bg-slate-50 transition"
                            >
                              수정
                            </button>
                            <button
                              onClick={() => handleDeleteClick("personal", lesson.id!)}
                              className="rounded-lg bg-white px-3 py-1.5 text-sm font-medium text-red-600 border border-red-200 hover:bg-red-50 transition"
                            >
                              삭제
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* ClinicRecord 섹션 */}
            {selectedDayData.clinicRecords.length > 0 && (
              <div>
                <h3 className="text-lg font-semibold text-slate-900 mb-3">클리닉 기록</h3>
                <div className="space-y-3">
                  {selectedDayData.clinicRecords.map((record) => (
                    <div
                      key={record.id}
                      className="rounded-lg border border-yellow-200 bg-yellow-50 p-4"
                    >
                      <p className="text-sm text-slate-600">{record.note}</p>
                      {record.writerRole && (
                        <p className="text-xs text-slate-500 mt-2">
                          작성자: {record.writerRole === "TEACHER" ? "선생님" : "조교"}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 모든 섹션이 비어있을 때 */}
            {selectedDayData.sharedLessons.length === 0 &&
              selectedDayData.personalLessons.length === 0 &&
              selectedDayData.clinicRecords.length === 0 && (
                <div className="text-center py-8 text-slate-500">
                  <p className="text-sm">이 날짜에는 기록이 없습니다.</p>
                </div>
              )}
          </div>
        </Modal>
      )}

      {/* 삭제 확인 다이얼로그 */}
      <ConfirmDialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        onConfirm={handleDeleteConfirm}
        title="삭제 확인"
        message={
          deletingItem?.type === "shared"
            ? "이 공통 진도를 삭제하시겠습니까?"
            : "이 개인 진도를 삭제하시겠습니까?"
        }
        confirmText="삭제"
        cancelText="취소"
        isLoading={deleteSharedLesson.isPending || deletePersonalLesson.isPending}
      />

      {/* 수정 모달 */}
      {editingItem && (
        <EditLessonModal
          open={true}
          onClose={handleEditCancel}
          onSave={handleEditSave}
          type={editingItem.type}
          initialData={editingItem.data}
          isLoading={updateSharedLesson.isPending || updatePersonalLesson.isPending}
        />
      )}
    </DashboardShell>
  );
}


function formatCourseNames(courseNames?: string[]) {
  const filtered = courseNames?.filter((name) => Boolean(name && name.trim())) ?? [];
  if (!filtered.length) {
    return "반 없음";
  }
  if (filtered.length <= 2) {
    return filtered.join(', ');
  }
  const [first, second] = filtered;
  return `${first}, ${second} 외 ${filtered.length - 2}개`;
}
export default function StudentCalendarPage() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen">로딩 중...</div>}>
      <StudentCalendarContent />
    </Suspense>
  );
}
