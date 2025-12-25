"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { ErrorState } from "@/components/ui/error-state";
import { StudentCalendarHeader } from "@/components/dashboard/calendar/student-calendar-header";
import { StudentInfoCard } from "@/components/dashboard/calendar/student-info-card";
import { MonthlyCalendarGrid } from "@/components/dashboard/calendar/monthly-calendar-grid";
import { CalendarDayDetailModal } from "@/components/dashboard/calendar/calendar-day-detail-modal";
import { ProgressEditModal } from "@/components/dashboard/progress/progress-edit-modal";
import { fetchStudentCourseRecords } from "@/lib/dashboard-api";
import { deleteCourseProgress, deletePersonalProgress, fetchStudentCalendar } from "@/lib/progress-api";
import { formatDateYmdKst } from "@/utils/date";
import type { StudentCourseListItemResponse } from "@/types/dashboard";
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
  courses: string[];
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

type StudentCalendarPageProps = {
  role: CalendarRole;
};

const SEARCH_DEBOUNCE_MS = 300;

export function StudentCalendarPage({ role }: StudentCalendarPageProps) {
  const { showToast } = useToast();
  const canEdit = role === "TEACHER";
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
    if (!searchValue.trim()) {
      setSearchResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        setSearchLoading(true);
        const response = await fetchStudentCourseRecords({
          status: "ACTIVE",
          keyword: searchValue.trim(),
          page: 0,
          size: 30
        });
        setSearchResults(groupStudentOptions(response.items));
      } catch (error) {
        const message = error instanceof Error ? error.message : "학생 검색에 실패했습니다.";
        showToast("error", message);
      } finally {
        setSearchLoading(false);
      }
    }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [searchValue, showToast]);

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

  const handleDeleteCourseProgress = async (id: string) => {
    if (!canEdit) {
      return;
    }
    try {
      await deleteCourseProgress(id);
      showToast("success", "진도 기록을 삭제했습니다.");
      await loadCalendar();
    } catch (error) {
      const message = error instanceof Error ? error.message : "진도를 삭제하지 못했습니다.";
      showToast("error", message);
    }
  };

  const handleDeletePersonalProgress = async (id: string) => {
    if (!canEdit) {
      return;
    }
    try {
      await deletePersonalProgress(id);
      showToast("success", "진도 기록을 삭제했습니다.");
      await loadCalendar();
    } catch (error) {
      const message = error instanceof Error ? error.message : "진도를 삭제하지 못했습니다.";
      showToast("error", message);
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
        canEdit={canEdit}
        onClose={() => setSelectedDateKey(null)}
        onEditCourse={(event) => {
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
        onDeleteCourse={(event) => event.id && void handleDeleteCourseProgress(event.id)}
        onDeletePersonal={(event) => event.id && void handleDeletePersonalProgress(event.id)}
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

      {calendarLoading ? (
        <ErrorState title="캘린더를 불러오는 중" description="잠시만 기다려 주세요." />
      ) : null}
    </div>
  );
}

function groupStudentOptions(records: StudentCourseListItemResponse[]): StudentSearchOption[] {
  const map = new Map<string, StudentSearchOption>();
  records.forEach((record) => {
    if (!record.studentMemberId) {
      return;
    }
    const existing = map.get(record.studentMemberId);
    const courseName = record.courseName ?? "반";
    if (existing) {
      if (!existing.courses.includes(courseName)) {
        existing.courses.push(courseName);
      }
      return;
    }
    map.set(record.studentMemberId, {
      studentId: record.studentMemberId,
      name: record.studentName ?? "학생",
      phoneNumber: record.phoneNumber ?? undefined,
      parentPhoneNumber: record.parentPhoneNumber ?? undefined,
      courses: [courseName]
    });
  });

  return Array.from(map.values());
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
