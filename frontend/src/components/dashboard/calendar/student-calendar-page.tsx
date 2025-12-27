"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { useSession } from "@/components/session/session-provider";
import { ErrorState } from "@/components/ui/error-state";
import { StudentCalendarHeader } from "@/components/dashboard/calendar/student-calendar-header";
import { StudentInfoCard } from "@/components/dashboard/calendar/student-info-card";
import { MonthlyCalendarGrid } from "@/components/dashboard/calendar/monthly-calendar-grid";
import { CalendarDayDetailModal } from "@/components/dashboard/calendar/calendar-day-detail-modal";
import { ProgressEditModal } from "@/components/dashboard/progress/progress-edit-modal";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { fetchTeacherStudents } from "@/lib/dashboard-api";
import {
  deleteClinicRecord,
  deleteCourseProgress,
  deletePersonalProgress,
  fetchStudentCalendar,
  updateClinicRecord
} from "@/lib/progress-api";
import { formatDateYmdKst } from "@/utils/date";
import { formatStudentGrade } from "@/utils/student";
import type { StudentSummaryResponse } from "@/types/dashboard";
import type {
  ClinicEvent,
  CourseProgressEvent,
  PersonalProgressEvent,
  StudentCalendarResponse
} from "@/types/progress";

type CalendarRole = "TEACHER" | "ASSISTANT";

type StudentSearchOption = {
  studentId: string;
  name: string;
  phoneNumber?: string;
  parentPhoneNumber?: string;
  schoolLabel?: string;
};

type DayEvents = {
  course: CourseProgressEvent[];
  personal: PersonalProgressEvent[];
  clinic: ClinicEvent[];
};

type DaySummary = {
  course?: EventSummary;
  personal?: EventSummary;
  clinic?: EventSummary;
};

type EventSummary = {
  title: string;
  extraCount: number;
};

type EditTarget = {
  type: "course" | "personal";
  id: string;
  courseId: string;
  recordId?: string;
  initialTitle?: string;
  initialContent?: string;
};

type ClinicEditFormState = {
  title: string;
  content: string;
  homeworkProgress: string;
};

type ClinicEditTarget = {
  recordId: string;
};

type StudentCalendarPageProps = {
  role: CalendarRole;
};

const SEARCH_DEBOUNCE_MS = 300;

export function StudentCalendarPage({ role }: StudentCalendarPageProps) {
  const { showToast } = useToast();
  const { member } = useSession();
  const memberId = member?.memberId ?? null;
  const isTeacher = member?.role === "TEACHER" || role === "TEACHER";
  const searchInputRef = useRef<HTMLInputElement>(null);
  const [searchValue, setSearchValue] = useState("");
  const [searchResults, setSearchResults] = useState<StudentSearchOption[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState<StudentSearchOption | null>(null);
  const [calendarData, setCalendarData] = useState<StudentCalendarResponse | null>(null);
  const [calendarLoading, setCalendarLoading] = useState(false);
  const [calendarError, setCalendarError] = useState<string | null>(null);
  const [currentMonth, setCurrentMonth] = useState<Date>(() => startOfMonth(new Date()));
  const [selectedDateKey, setSelectedDateKey] = useState<string | null>(null);
  const [editTarget, setEditTarget] = useState<EditTarget | null>(null);
  const [clinicEditTarget, setClinicEditTarget] = useState<ClinicEditTarget | null>(null);
  const [clinicEditForm, setClinicEditForm] = useState<ClinicEditFormState>({
    title: "",
    content: "",
    homeworkProgress: ""
  });
  const [clinicEditError, setClinicEditError] = useState<string | null>(null);
  const [clinicEditLoading, setClinicEditLoading] = useState(false);

  const monthLabel = formatMonthLabel(currentMonth);
  const canMovePrev = canMoveMonth(currentMonth, -1);
  const canMoveNext = canMoveMonth(currentMonth, 1);

  const eventsByDate = useMemo(() => buildEventsByDate(calendarData), [calendarData]);
  const daySummaries = useMemo(() => buildDaySummaries(eventsByDate), [eventsByDate]);
  const calendarDays = useMemo(() => buildCalendarDays(currentMonth), [currentMonth]);

  const selectedDayEvents = selectedDateKey ? eventsByDate.get(selectedDateKey) ?? emptyDayEvents() : null;

  const loadCalendar = useCallback(async () => {
    if (!selectedStudent) {
      setCalendarData(null);
      return;
    }
    setCalendarLoading(true);
    setCalendarError(null);
    try {
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;
      const data = await fetchStudentCalendar({ studentId: selectedStudent.studentId, year, month });
      setCalendarData(data);
    } catch (error) {
      const message = error instanceof Error ? error.message : "학생 캘린더를 불러오지 못했습니다.";
      setCalendarError(message);
      showToast("error", message);
    } finally {
      setCalendarLoading(false);
    }
  }, [currentMonth, selectedStudent, showToast]);

  useEffect(() => {
    void loadCalendar();
  }, [loadCalendar]);

  useEffect(() => {
    setSelectedDateKey(null);
  }, [currentMonth, selectedStudent]);

  useEffect(() => {
    const trimmed = searchValue.trim();
    if (!trimmed) {
      setSearchResults([]);
      return;
    }
    if (selectedStudent && trimmed === selectedStudent.name) {
      setSearchResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        setSearchLoading(true);
        const response = await fetchTeacherStudents({
          keyword: trimmed,
          page: 0,
          size: 30
        });
        setSearchResults(toStudentOptions(response.items));
      } catch (error) {
        const message = error instanceof Error ? error.message : "학생 검색에 실패했습니다.";
        showToast("error", message);
      } finally {
        setSearchLoading(false);
      }
    }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [searchValue, selectedStudent, showToast]);

  const handleStudentSelect = (option: StudentSearchOption) => {
    setSelectedStudent(option);
    setSearchValue(option.name);
    setSearchResults([]);
    setSelectedDateKey(null);
  };

  const handleMonthChange = (offset: number) => {
    if (!canMoveMonth(currentMonth, offset)) {
      return;
    }
    setCurrentMonth((prev) => startOfMonth(addMonths(prev, offset)));
  };

  const canEditCourse = useCallback(
    (event: CourseProgressEvent) => {
      if (isTeacher) {
        return true;
      }
      if (!memberId) {
        return false;
      }
      return event.writerId === memberId;
    },
    [isTeacher, memberId]
  );

  const canEditPersonal = useCallback(
    (event: PersonalProgressEvent) => {
      if (isTeacher) {
        return true;
      }
      if (!memberId) {
        return false;
      }
      return event.writerId === memberId;
    },
    [isTeacher, memberId]
  );

  const canEditClinic = useCallback(
    (event: ClinicEvent) => {
      if (isTeacher) {
        return true;
      }
      if (!memberId) {
        return false;
      }
      return event.recordSummary?.writerId === memberId;
    },
    [isTeacher, memberId]
  );

  const handleDeleteCourseProgress = async (event: CourseProgressEvent) => {
    if (!event.id || !canEditCourse(event)) {
      return;
    }
    try {
      await deleteCourseProgress(event.id);
      showToast("success", "진도 기록을 삭제했습니다.");
      await loadCalendar();
    } catch (error) {
      const message = error instanceof Error ? error.message : "진도를 삭제하지 못했습니다.";
      showToast("error", message);
    }
  };

  const handleDeletePersonalProgress = async (event: PersonalProgressEvent) => {
    if (!event.id || !canEditPersonal(event)) {
      return;
    }
    try {
      await deletePersonalProgress(event.id);
      showToast("success", "진도 기록을 삭제했습니다.");
      await loadCalendar();
    } catch (error) {
      const message = error instanceof Error ? error.message : "진도를 삭제하지 못했습니다.";
      showToast("error", message);
    }
  };

  const handleDeleteClinicRecord = async (event: ClinicEvent) => {
    const recordId = event.recordSummary?.id;
    if (!recordId || !canEditClinic(event)) {
      return;
    }
    try {
      await deleteClinicRecord(recordId);
      showToast("success", "클리닉 기록을 삭제했습니다.");
      await loadCalendar();
    } catch (error) {
      const message = error instanceof Error ? error.message : "클리닉 기록을 삭제하지 못했습니다.";
      showToast("error", message);
    }
  };

  const handleSaveClinicRecord = async () => {
    if (!clinicEditTarget) {
      return;
    }
    if (!clinicEditForm.title.trim() || !clinicEditForm.content.trim()) {
      setClinicEditError("제목과 내용을 입력해주세요.");
      return;
    }
    setClinicEditLoading(true);
    setClinicEditError(null);
    try {
      await updateClinicRecord(clinicEditTarget.recordId, {
        title: clinicEditForm.title,
        content: clinicEditForm.content,
        homeworkProgress: clinicEditForm.homeworkProgress || undefined
      });
      showToast("success", "클리닉 기록을 수정했습니다.");
      setClinicEditTarget(null);
      await loadCalendar();
    } catch (error) {
      const message = error instanceof Error ? error.message : "클리닉 기록을 수정하지 못했습니다.";
      setClinicEditError(message);
      showToast("error", message);
    } finally {
      setClinicEditLoading(false);
    }
  };

  if (calendarError && !calendarData) {
    return (
      <ErrorState
        title="캘린더를 불러오지 못했습니다"
        description={calendarError}
        onRetry={() => void loadCalendar()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <StudentCalendarHeader
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        searchResults={searchResults}
        searchLoading={searchLoading}
        onSelectStudent={handleStudentSelect}
        inputRef={searchInputRef}
      />

      <StudentInfoCard
        student={selectedStudent}
        onChangeStudent={() => searchInputRef.current?.focus()}
      />

      <MonthlyCalendarGrid
        days={calendarDays}
        summaries={daySummaries}
        selectedKey={selectedDateKey}
        onSelectDate={(key) => setSelectedDateKey(key)}
        monthLabel={monthLabel}
        onPrevMonth={() => handleMonthChange(-1)}
        onNextMonth={() => handleMonthChange(1)}
        canMovePrev={canMovePrev}
        canMoveNext={canMoveNext}
      />

      <CalendarDayDetailModal
        open={Boolean(selectedDateKey)}
        dateKey={selectedDateKey}
        events={selectedDayEvents}
        onClose={() => setSelectedDateKey(null)}
        canEditCourse={canEditCourse}
        canEditPersonal={canEditPersonal}
        canEditClinic={canEditClinic}
        onEditCourse={(event) => {
          if (!canEditCourse(event)) {
            return;
          }
          if (!event.id || !event.courseId) {
            return;
          }
          setEditTarget({
            type: "course",
            id: event.id,
            courseId: event.courseId,
            initialTitle: event.title ?? "",
            initialContent: event.content ?? ""
          });
        }}
        onEditPersonal={(event) => {
          if (!canEditPersonal(event)) {
            return;
          }
          if (!event.id || !event.courseId || !event.studentCourseRecordId) {
            return;
          }
          setEditTarget({
            type: "personal",
            id: event.id,
            courseId: event.courseId,
            recordId: event.studentCourseRecordId,
            initialTitle: event.title ?? "",
            initialContent: event.content ?? ""
          });
        }}
        onEditClinic={(event) => {
          if (!canEditClinic(event)) {
            return;
          }
          const recordSummary = event.recordSummary;
          if (!recordSummary?.id) {
            return;
          }
          setClinicEditTarget({ recordId: recordSummary.id });
          setClinicEditForm({
            title: recordSummary.title ?? "",
            content: recordSummary.content ?? "",
            homeworkProgress: recordSummary.homeworkProgress ?? ""
          });
          setClinicEditError(null);
        }}
        onDeleteCourse={(event) => void handleDeleteCourseProgress(event)}
        onDeletePersonal={(event) => void handleDeletePersonalProgress(event)}
        onDeleteClinic={(event) => void handleDeleteClinicRecord(event)}
      />

      <ProgressEditModal
        open={Boolean(editTarget)}
        target={editTarget}
        onClose={() => setEditTarget(null)}
        onSaved={async () => {
          setEditTarget(null);
          await loadCalendar();
        }}
      />

      <Modal
        open={Boolean(clinicEditTarget)}
        onClose={() => {
          if (!clinicEditLoading) {
            setClinicEditTarget(null);
          }
        }}
        title="클리닉 기록 수정"
        size="md"
      >
        <div className="space-y-4">
          <TextField
            label="제목"
            value={clinicEditForm.title}
            onChange={(event) => setClinicEditForm((prev) => ({ ...prev, title: event.target.value }))}
          />
          <label className="flex flex-col gap-2 text-sm text-slate-700">
            내용
            <textarea
              value={clinicEditForm.content}
              onChange={(event) => setClinicEditForm((prev) => ({ ...prev, content: event.target.value }))}
              className="min-h-[140px] rounded-xl border border-slate-200 px-3 py-2 text-sm"
            />
          </label>
          <TextField
            label="과제 진행"
            value={clinicEditForm.homeworkProgress}
            onChange={(event) => setClinicEditForm((prev) => ({ ...prev, homeworkProgress: event.target.value }))}
          />
          {clinicEditError && <InlineError message={clinicEditError} />}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setClinicEditTarget(null)} disabled={clinicEditLoading}>
              취소
            </Button>
            <Button onClick={() => void handleSaveClinicRecord()} disabled={clinicEditLoading}>
              {clinicEditLoading ? "저장 중..." : "저장"}
            </Button>
          </div>
        </div>
      </Modal>

      {calendarLoading ? (
        <ErrorState title="캘린더를 불러오는 중" description="잠시만 기다려 주세요." />
      ) : null}
    </div>
  );
}

function toStudentOptions(students: StudentSummaryResponse[]): StudentSearchOption[] {
  const seen = new Set<string>();
  return students
    .filter((student) => Boolean(student.memberId))
    .map((student) => ({
      studentId: student.memberId ?? "",
      name: student.name ?? "학생",
      phoneNumber: student.phoneNumber ?? undefined,
      parentPhoneNumber: student.parentPhone ?? undefined,
      schoolLabel: formatStudentSummary(student)
    }))
    .filter((option) => {
      if (!option.studentId) {
        return true;
      }
      if (seen.has(option.studentId)) {
        return false;
      }
      seen.add(option.studentId);
      return true;
    });
}

function formatStudentSummary(student: StudentSummaryResponse) {
  const schoolName = student.schoolName ?? "학교 정보 없음";
  const gradeLabel = formatStudentGrade(student.grade);
  if (!gradeLabel) {
    return schoolName;
  }
  return `${schoolName}(${gradeLabel})`;
}

function buildEventsByDate(calendarData: StudentCalendarResponse | null): Map<string, DayEvents> {
  const map = new Map<string, DayEvents>();
  if (!calendarData) {
    return map;
  }
  calendarData.courseProgress?.forEach((event) => {
    if (!event.date) return;
    const bucket = map.get(event.date) ?? emptyDayEvents();
    bucket.course.push(event);
    map.set(event.date, bucket);
  });
  calendarData.personalProgress?.forEach((event) => {
    if (!event.date) return;
    const bucket = map.get(event.date) ?? emptyDayEvents();
    bucket.personal.push(event);
    map.set(event.date, bucket);
  });
  calendarData.clinicEvents?.forEach((event) => {
    if (!event.date) return;
    if (!event.recordSummary) return;
    const bucket = map.get(event.date) ?? emptyDayEvents();
    bucket.clinic.push(event);
    map.set(event.date, bucket);
  });
  return map;
}

function emptyDayEvents(): DayEvents {
  return { course: [], personal: [], clinic: [] };
}

function buildDaySummaries(eventsByDate: Map<string, DayEvents>): Record<string, DaySummary> {
  const summaries: Record<string, DaySummary> = {};
  eventsByDate.forEach((value, key) => {
    summaries[key] = {
      course: buildCategorySummary(value.course, (event) =>
        buildEventTitle(event.courseName, event.title)
      ),
      personal: buildCategorySummary(value.personal, (event) =>
        buildEventTitle(event.courseName, event.title)
      ),
      clinic: buildCategorySummary(value.clinic, (event) => event.recordSummary?.title ?? "클리닉 기록")
    };
  });
  return summaries;
}

function buildCategorySummary<T>(events: T[], titleResolver: (event: T) => string): EventSummary | undefined {
  if (events.length === 0) {
    return undefined;
  }
  return {
    title: titleResolver(events[0]) ?? "기록",
    extraCount: events.length > 1 ? events.length - 1 : 0
  };
}

function buildEventTitle(courseName?: string | null, title?: string | null) {
  if (courseName && title) {
    return `${courseName} ${title}`;
  }
  return title ?? courseName ?? "기록";
}

function buildCalendarDays(current: Date) {
  const start = startOfMonth(current);
  const end = endOfMonth(current);
  const startWeekday = (start.getDay() + 6) % 7;
  const totalCells = 42;
  const days: { key: string; date: Date; isCurrentMonth: boolean }[] = [];

  const firstCellDate = new Date(start);
  firstCellDate.setDate(start.getDate() - startWeekday);

  for (let i = 0; i < totalCells; i += 1) {
    const date = new Date(firstCellDate);
    date.setDate(firstCellDate.getDate() + i);
    const key = formatDateKey(date);
    days.push({
      key,
      date,
      isCurrentMonth: date >= start && date <= end
    });
  }

  return days;
}

function formatDateKey(date: Date) {
  return formatDateYmdKst(date);
}

function formatMonthLabel(date: Date) {
  return date.toLocaleDateString("ko-KR", { year: "numeric", month: "long" });
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

function addMonths(date: Date, offset: number) {
  return new Date(date.getFullYear(), date.getMonth() + offset, 1);
}

function canMoveMonth(date: Date, offset: number) {
  const candidate = addMonths(date, offset);
  const now = new Date();
  const diff = (candidate.getFullYear() - now.getFullYear()) * 12 + (candidate.getMonth() - now.getMonth());
  return diff >= -3 && diff <= 3;
}
